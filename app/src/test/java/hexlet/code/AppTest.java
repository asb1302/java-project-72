package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.ebean.Database;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


class AppTest {

    private static Javalin app;
    private static String baseUrl;
    private static Database database;

    protected static String getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName).toAbsolutePath().toString();
    }

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @Test
    void testRoot() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testUrls() {
        String urlStr1 = "https://one-domain.org";
        String urlStr2 = "https://two-domain.org:8080";

        Url url1 = new Url(urlStr1);
        url1.save();

        Url url2 = new Url(urlStr2);
        url2.save();

        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);

        assertThat(content).contains(urlStr1);
        assertThat(content).contains(urlStr2);
    }

    @Test
    void testNew() { // TODO: Дописать
    }

    @Test
    void testCreateUrl() {
        String url = "https://stackoverflow.com";

        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", url)
                .asEmpty();

        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        Url actualUrl = new QUrl()
                .name.equalTo(url)
                .findOne();

        assertThat(actualUrl).isNotNull();
        assertThat(actualUrl.getName()).isEqualTo(url);
    }

    @Test
    void testShow() {
        String urlStr = "https://one-domain.org";

        Url url = new Url(urlStr);
        url.save();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/1")
                .asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains(urlStr);
    }

    @Test
    void testChecks() throws IOException {
        String simplePageHtml = Files.readString(Paths.get(getFixturePath("simple-page.html")));

        MockWebServer server = new MockWebServer();
        String urlStr = server.url("/").toString();
        server.enqueue(new MockResponse().setBody(simplePageHtml));

        Url url = new Url(urlStr);
        url.save();

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls/" + url.getId() + "/checks")
                .asEmpty();

        assertThat(response.getStatus()).isEqualTo(200);

        UrlCheck urlCheck = new QUrlCheck()
                .url.equalTo(url)
                .findOne();

        assertThat(urlCheck).isNotNull();
        assertThat(urlCheck.getTitle()).isEqualTo("Title text");
        assertThat(urlCheck.getDescription()).isEqualTo("Description content");
        assertThat(urlCheck.getH1()).isEqualTo("H1 text");
    }
}
