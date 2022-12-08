package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
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

    protected static String getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName).toAbsolutePath().toString();
    }

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
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
        UrlCheck urlCheck1 = new UrlCheck(
                403,
                null,
                null,
                null,
                url1
        );
        urlCheck1.save();
        UrlCheck urlCheck2 = new UrlCheck(
                200,
                "title1",
                "h1",
                "description",
                url1
        );
        urlCheck2.save();

        Url url2 = new Url(urlStr2);
        url2.save();

        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);

        assertThat(content).contains(urlStr1);
        assertThat(content).contains(urlStr1);

        assertThat(content).contains(urlCheck2.getStatusCode().toString());
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

        assertThat(response.getStatus()).isEqualTo(302);

        UrlCheck urlCheck = new QUrlCheck()
                .url.equalTo(url)
                .findOne();

        assertThat(urlCheck).isNotNull();
        assertThat(urlCheck.getTitle()).isEqualTo("Title text");
        assertThat(urlCheck.getDescription()).isEqualTo("Description content");
        assertThat(urlCheck.getH1()).isEqualTo("H1 text");
    }
}
