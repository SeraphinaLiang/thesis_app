package com.example.solidapp;

import com.inrupt.client.Request;
import com.inrupt.client.Response;
import com.inrupt.client.auth.Session;
import com.inrupt.client.openid.OpenIdSession;
import com.inrupt.client.solid.SolidSyncClient;
import com.inrupt.client.webid.WebIdProfile;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.net.URI;
import java.util.*;

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
    String storage = "71416ab6-3dc3-49aa-8ed8-57c633705618";

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

    @PostMapping("/test")
    public String test(@RequestBody JSONObject obj) {

        return "ok";
    }

    /**
     * API POST  http://localhost:8080/secure , call by client to perform a secure request
     * @param obj a json object contains 1. the encrypted identifier 2. pk0 (the first array item of the public key)
     * @return the encrypted target file sequence
     */
    @PostMapping("/secure")
    public ResponseEntity<ArrayList<String>> secureRequest(@RequestBody JSONObject obj) {

        //  pk0 & the encrypted id
        String pk0 = (String) obj.get("pk0");
        @SuppressWarnings("unchecked")
        ArrayList<String> encryptedIdentifier = (ArrayList<String>) obj.get("encryptedIdentifier");

        // get all the file on the pod
        HashSet<String> filesnamehm = getFilesName();
        HashMap<String, String> files = new HashMap<>(); // <plaintext_identifier, encrypted_content>

        ArrayList<String> filenames = new ArrayList<>(filesnamehm);

        for (int i = 0; i < filenames.size(); i++) {
            Request request = Request.newBuilder()
                    .uri(URI.create(podStorageCrypto + "/" + filenames.get(i)))
                    .header("Accept", "text/plain")
                    .GET()
                    .build();
            Response<String> response = client.send(
                    request,
                    Response.BodyHandlers.ofString());
            files.put(filenames.get(i), response.body());
        }

        //  printWriter.println("files: "+ files);
        ArrayList<String> result = computation(encryptedIdentifier, files, pk0);

        return ResponseEntity.ok(result);
    }

    /**
     * perform DGHV computation
     * @param encryptIdentifier the encrypted identifier of the target file
     * @param files all the existing files on Solid Pod under /crypto
     * @param pk0   the first array item of the public key , use for add/multiply
     * @return encrypted target file sequence
     */
    public ArrayList<String> computation(ArrayList<String> encryptIdentifier, HashMap<String, String> files, String pk0) {

        ArrayList<String> result = new ArrayList<>();
        result.add("hello");
        result.add("world");

        return result;
    }

    /**
     * get all the file names on Solid Pod under directory /crypto
     * @return HashSet<String> filenames
     */
    public HashSet<String> getFilesName() {
        Request request = Request.newBuilder()
                .uri(URI.create(podStorageCrypto))
                .header("Accept", "*")
                .GET()
                .build();
        Response<String> response = client.send(
                request,
                Response.BodyHandlers.ofString());

        String[] msg1 = response.body().split("\n");
        ArrayList<String> handle1 = new ArrayList<>();
        for (String s : msg1) {
            handle1.addAll(List.of(s.split(" ")));
        }
        // printWriter.println(handle1);
        ArrayList<String> handle2 = new ArrayList<>();
        for (String s : handle1) {
            if (s.contains(storage)) handle2.add(s);
        }
        HashSet<String> files = new HashSet<>();

        for (String s : handle2) {
            String s1 = s.substring(1, s.length() - 1);
            String[] sq = s1.split("/");
            files.add(sq[3]);
        }
        files.remove(storage);
        // [00111100.txt, 10110001.txt, 10011001.txt]
        return files;
    }

    /**
     * get a resource from pod
     * @param url the url of the resource
     * @return the resource
     */
    @GetMapping("/resource/get")
    public String getResource(@RequestParam(value = "url") String url) {
        Request request = Request.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "text/plain")
                .GET()
                .build();
        Response<String> response = client.send(
                request,
                Response.BodyHandlers.ofString());
        return response.body();
    }

}
