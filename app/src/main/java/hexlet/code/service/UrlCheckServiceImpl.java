package hexlet.code.service;

import hexlet.code.domain.UrlCheckResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.server.Response;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlCheckServiceImpl implements UrlCheckServiceInterface {
    private static String TITLE_REGEX = "<title>(.*?)</title>";
    private static String H1_REGEX = "<h1.*>(.*?)<\\/h1>";
    private static String META_DESCRIPTION_REGEX = "<meta[^>]*name=[\\\"|\\']description[\\\"|\\'][^>]*content=[\\\"]([^\\\"]*)[\\\"][^>]*>";
    @Override
    public UrlCheckResponse check(String url) {
        UrlCheckResponse checkResponse = new UrlCheckResponse();

        HttpResponse<String> response = Unirest.get(url).asString();
        checkResponse.statusCode = response.getStatus();

        if (response.getStatus() == Response.SC_OK) {
            String html = response.getBody();
            html = html.replaceAll("\\s+", " ");

            checkResponse.title = findTagContent(html, TITLE_REGEX);
            checkResponse.h1 = findTagContent(html, H1_REGEX);
            checkResponse.description = findTagContent(html, META_DESCRIPTION_REGEX);
        }

        return checkResponse;
    }

    private String findTagContent(String html, String regex) {
        String content = null;

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(html);

        if (m.find() == true) {
            content = m.group(1);
        }

        return content.trim();
    }
}
