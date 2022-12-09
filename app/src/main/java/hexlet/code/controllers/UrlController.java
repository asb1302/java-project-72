package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.PagedList;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.core.validation.ValidationError;
import io.javalin.core.validation.Validator;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.server.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UrlController {
    public static Handler listUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;

        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        int currentPage = pagedUrls.getPageIndex() + 1;

        List<Url> urls = pagedUrls.getList();


        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };

    public static Handler createUrl = ctx -> {
        String urlValue = ctx.formParam("url");


        Validator<String> urlValidator = ctx.formParamAsClass("url", String.class)
                .check(it -> !it.isEmpty(), "url не должно быть пустым");

        Map<String, List<ValidationError<? extends Object>>> errors = JavalinValidation.collectErrors(
                urlValidator
        );

        if (!errors.isEmpty()) {
            ctx.status(422);
            ctx.attribute("errors", errors);
            Url invalidUrl = new Url(urlValue);
            ctx.attribute("url", invalidUrl);
            ctx.render("urls/new.html");
            return;
        }

        try {
            URL urlParser = new URL(urlValue);
            String newName = urlParser.getProtocol() + "://" + urlParser.getAuthority();

            Url existedUrl = new QUrl()
                    .name.equalTo(newName)
                    .findOne();

            if (null != existedUrl) {
                ctx.sessionAttribute("flash", "Сайт уже добавлен!");
                ctx.sessionAttribute("flash-type", "danger");
                ctx.redirect("/urls");
                return;
            }

            Url url = new Url(newName);
            url.save();
        } catch (Exception exception) {
            ctx.sessionAttribute("flash", "Некорректный сайт!");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/urls");
            return;
        }

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("/urls");
    };

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.equalTo(url)
                .findList();

        ctx.attribute("url", url);
        ctx.attribute("urlChecks", urlChecks);
        ctx.render("urls/show.html");
    };

    public static Handler checks = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        try {
            HttpResponse<String> response = Unirest
                    .get(url.getName())
                    .asString();

            String content = response.getBody();

            int statusCode = response.getStatus();
            String title = null;
            String h1 = null;
            String description = null;

            Document body = Jsoup.parse(content);
            title = body.title();

            if (response.getStatus() == Response.SC_OK) {
                h1 = body.selectFirst("h1") != null
                        ? Objects.requireNonNull(body.selectFirst("h1")).text()
                        : null;
                description = body.selectFirst("meta[name=description]") != null
                        ? Objects.requireNonNull(body.selectFirst("meta[name=description]")).attr("content")
                        : null;
            }

            UrlCheck urlCheck = new UrlCheck(
                    statusCode,
                    title,
                    h1,
                    description,
                    url
            );
            urlCheck.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
            ctx.redirect("/urls/" + url.getId());
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Произошла ошибка при проверке сайта!");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/urls/" + url.getId());
        }
    };
}
