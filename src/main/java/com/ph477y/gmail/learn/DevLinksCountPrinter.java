package com.ph477y.gmail.learn;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This is a toy project to learn a little bit about the GMail SDK
 * -https://developers.google.com/gmail/api/guides
 * This class is basically a clone of the GMailQuickstart sample
 *
 * I email myself links (usually from hacker news) with the intent to
 * read the details of the articles later. Unfortunately I rarely get back to them
 * I thought I'd use this API to find out how far behind I Am (Maybe I didn't actually want to know)
 */
public class DevLinksCountPrinter {
  private static final String APPLICATION_NAME = "DevLinks Count Printer";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved tokens/ folder.
   */
  private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_LABELS);
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    // Load client secrets.
    InputStream in = DevLinksCountPrinter.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
    }
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  public static final String LBL_TO_TRACK ="dev_link";
  public static void main(String... args) throws IOException, GeneralSecurityException {
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build();

    String user = "me";
    Gmail.Users.Labels labels = service.users().labels();
    ListLabelsResponse listResponse = labels.list(user).execute();
    List<Label> userLabels = listResponse.getLabels();

    if (userLabels.isEmpty()) {
      System.out.println("No labels found.");
    } else {
      //Map<String, String> labelNameToIds = userLabels.stream().collect(Collectors.toMap(Label::getName, Label::getId, (oldValue, newValue) -> newValue));
      //System.out.println("Labels:");
      for (Label label : userLabels) {
        if (Objects.equals(label.getName(), LBL_TO_TRACK)) {
          Label devLinkLabel = labels.get(user, label.getId()).execute();
          System.out.println("*******************************");
          System.out.println("Dev-Links counts");
          System.out.printf("%s - %s\n",devLinkLabel.getMessagesUnread(), devLinkLabel.getMessagesTotal());
          System.out.printf("You've only read %s\n of the links you thought you'd be interested in\n", (devLinkLabel.getMessagesTotal() - devLinkLabel.getMessagesUnread()));
          System.out.println("*******************************");
        } else {
            //System.out.printf("- %s\n", label.getName());
        }
      }
    }
  }
}