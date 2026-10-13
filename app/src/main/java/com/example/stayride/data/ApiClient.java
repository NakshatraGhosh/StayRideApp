package com.example.stayride.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.stayride.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    // Configured in gradle.properties (stayride.baseUrl) -> BuildConfig.
    public static final String BASE_URL = BuildConfig.BASE_URL;

    private static ApiClient instance;

    public static void init(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
    }

    public static ApiClient get() {
        return instance;
    }

    private final Context context;
    private final SharedPreferences prefs;
    private String accessToken;
    private String refreshToken;
    private String ownerName;
    private String homestayName;

    private ApiClient(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("stayride_session", Context.MODE_PRIVATE);
        this.accessToken = prefs.getString("accessToken", null);
        this.refreshToken = prefs.getString("refreshToken", null);
        this.ownerName = prefs.getString("ownerName", null);
        this.homestayName = prefs.getString("homestayName", null);
    }

    public Context context() {
        return context;
    }

    public boolean isLoggedIn() {
        return accessToken != null;
    }

    public String ownerName() {
        return ownerName;
    }

    public String homestayName() {
        return homestayName;
    }

    public void saveAuth(JSONObject auth) {
        accessToken = Json.str(auth, "accessToken");
        refreshToken = Json.str(auth, "refreshToken");
        JSONObject owner = auth.optJSONObject("owner");
        JSONObject homestay = auth.optJSONObject("homestay");
        if (owner != null) {
            String n = Json.str(owner, "name");
            if (n != null) ownerName = n;
        }
        if (homestay != null) {
            String n = Json.str(homestay, "name");
            if (n != null) homestayName = n;
        }
        prefs.edit()
                .putString("accessToken", accessToken)
                .putString("refreshToken", refreshToken)
                .putString("ownerName", ownerName)
                .putString("homestayName", homestayName)
                .apply();
    }

    public void clearAuth() {
        accessToken = null;
        refreshToken = null;
        ownerName = null;
        homestayName = null;
        prefs.edit().clear().apply();
    }

    public JSONObject post(String path, JSONObject body) {
        return parseObject(execute("POST", path, body));
    }

    public JSONObject put(String path, JSONObject body) {
        return parseObject(execute("PUT", path, body));
    }

    public JSONObject patch(String path, JSONObject body) {
        return parseObject(execute("PATCH", path, body));
    }

    public JSONObject get(String path) {
        return parseObject(execute("GET", path, null));
    }

    public JSONObject delete(String path) {
        return parseObject(execute("DELETE", path, null));
    }

    public JSONArray getArray(String path) {
        return parseArray(execute("GET", path, null));
    }

    private static JSONObject parseObject(String text) {
        try {
            return new JSONObject(text);
        } catch (JSONException e) {
            throw new ApiException(0, e.getMessage());
        }
    }

    private static JSONArray parseArray(String text) {
        try {
            return new JSONArray(text);
        } catch (JSONException e) {
            throw new ApiException(0, e.getMessage());
        }
    }

    private String execute(String method, String path, JSONObject body) {
        try {
            return send(method, path, body, false);
        } catch (Unauthorized e) {
            if (tryRefresh()) {
                try {
                    return send(method, path, body, true);
                } catch (ApiException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new ApiException(0, ex.getMessage());
                }
            }
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(0, e.getMessage());
        }
    }

    private String send(String method, String path, JSONObject body, boolean retried) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        c.setRequestMethod(method);
        c.setRequestProperty("Accept", "application/json");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        if (accessToken != null) {
            c.setRequestProperty("Authorization", "Bearer " + accessToken);
        }
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            OutputStream os = c.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = read(is);
        if (code == 401) {
            throw new Unauthorized(extractMessage(text));
        }
        if (code >= 400) {
            throw new ApiException(code, extractMessage(text));
        }
        return text;
    }

    private boolean tryRefresh() {
        if (refreshToken == null) {
            clearAuth();
            return false;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("refreshToken", refreshToken);
            HttpURLConnection c = (HttpURLConnection) new URL(BASE_URL + "/auth/refresh").openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setDoOutput(true);
            OutputStream os = c.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            int code = c.getResponseCode();
            String text = read(code >= 400 ? c.getErrorStream() : c.getInputStream());
            if (code >= 400) {
                clearAuth();
                return false;
            }
            saveAuth(new JSONObject(text));
            return accessToken != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractMessage(String text) {
        if (text == null || text.isEmpty()) return "Request failed";
        try {
            JSONObject o = new JSONObject(text);
            JSONObject err = o.optJSONObject("error");
            if (err != null) {
                String msg = Json.str(err, "message");
                if (msg != null) return msg;
            }
            String msg = Json.str(o, "message");
            if (msg != null) return msg;
        } catch (Exception ignored) {
        }
        return text;
    }

    private static String read(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line);
        }
        r.close();
        return sb.toString();
    }

    public static class ApiException extends RuntimeException {
        public final int code;

        public ApiException(int code, String message) {
            super(message == null ? "Network error" : message);
            this.code = code;
        }
    }

    private static class Unauthorized extends ApiException {
        Unauthorized(String message) {
            super(401, message);
        }
    }
}
