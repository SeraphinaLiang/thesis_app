package com.example.solidapp;

import com.inrupt.client.Request;
import com.inrupt.client.Response;
import com.inrupt.client.auth.Session;
import com.inrupt.client.openid.OpenIdSession;
import com.inrupt.client.solid.SolidSyncClient;
import com.inrupt.client.webid.WebIdProfile;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/")
@RestController
public class Controller {

    /**
     * Note 1: Authenticated Session
     * Using the client credentials, create an authenticated session.
     * <p>
     * Your WebID is
     * https://id.inrupt.com/seraphina
     * <p>
     * This is how sites and apps will recognize you.
     * You can store data in your Solid Pod at
     * https://storage.inrupt.com/71416ab6-3dc3-49aa-8ed8-57c633705618/
     * <p>
     * Client ID
     * 61351aea-6eba-4e87-be0a-b1a1f4205935
     * <p>
     * Client Secret
     * b4bd0f7d-1336-4e18-a3eb-ac56e6dfc50d
     */
    String clientId = "61351aea-6eba-4e87-be0a-b1a1f4205935";
    String clientSecret = "b4bd0f7d-1336-4e18-a3eb-ac56e6dfc50d";
    String podStorageCrypto = "https://storage.inrupt.com/71416ab6-3dc3-49aa-8ed8-57c633705618/crypto";

    final Session session = OpenIdSession.ofClientCredentials(
            URI.create("https://login.inrupt.com"),
            clientId,
            clientSecret,
            "client_secret_basic");
    /**
     * Note 2: SolidSyncClient
     * Instantiates a synchronous client for the authenticated session.
     * The client has methods to perform CRUD operations.
     */
    final SolidSyncClient client = SolidSyncClient.getClient().session(session);
    private final PrintWriter printWriter = new PrintWriter(System.out, true);

    /**
     * Note 3: SolidSyncClient.read()
     * Using the SolidSyncClient client.read() method, reads the user's WebID Profile document and returns the Pod URI(s).
     */
    @GetMapping("/pods")
    public Set<URI> getPods(@RequestParam(value = "webid", defaultValue = "") String webID) {
        printWriter.println("Controller:: getPods");
        System.out.println(webID);
        try (final var profile = client.read(URI.create(webID), WebIdProfile.class)) {
            return profile.getStorage();
        }
    }

    /**
     *
     */
    @PostMapping ("/secure")
    public String secureRequest(@RequestBody String encryptedIdentifier) {
        ArrayList<String> filenames = getFilesName();
        HashMap<String,String> files = new HashMap<>();
        printWriter.println("encryptedIdentifier: "+encryptedIdentifier);

        for(int i=0;i< filenames.size();i++){
            Request request = Request.newBuilder()
                    .uri(URI.create(podStorageCrypto+"/"+filenames.get(i)))
                    .header("Accept", "text/plain")
                    .GET()
                    .build();
            Response<String> response = client.send(
                    request,
                    Response.BodyHandlers.ofString());
            files.put(filenames.get(i),response.body());
        }

        printWriter.println("files: "+ files);

        return files.toString();
    }

    @GetMapping("/resource/get")
    public String getResourceAsTurtle(@RequestParam(value = "resourceURL") String resourceURL) {
        Request request = Request.newBuilder()
                .uri(URI.create(resourceURL))
                .header("Accept", "text/plain")
                .GET()
                .build();
        Response<String> response = client.send(
                request,
                Response.BodyHandlers.ofString());
        return response.body();
    }


    public ArrayList<String> getFilesName() {
        Request request = Request.newBuilder()
                .uri(URI.create(podStorageCrypto))
                .header("Accept", "*")
                .GET()
                .build();
        Response<String> response = client.send(
                request,
                Response.BodyHandlers.ofString());

        String[] msg1 = response.body().split("\n");
        String[] msg2 = msg1[8].split(" ");
        ArrayList<String> list = new ArrayList<>();
        ArrayList<String> files = new ArrayList<>();

        for (String s : msg2) {
            if (s.contains("crypto")) list.add(s);
        }
        for (String s: list) {
            String s1 = s.substring(1,s.length()-1);
            String[] sq = s1.split("/");
            files.add(sq[3]);
        }
          // [00111100.txt, 10110001.txt, 10011001.txt]
        return files;
    }
}
