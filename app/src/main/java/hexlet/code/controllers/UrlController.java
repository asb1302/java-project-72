package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.core.validation.ValidationError;
import io.javalin.core.validation.Validator;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class UrlController {
    public static Handler listUrls = ctx -> {

        List<Url> urls = new QUrl()
                .orderBy()
                .createdAt.desc()
                .findList();

        ctx.attribute("urls", urls);
        ctx.render("urls/index.html");
    };

    public static Handler newUrl = ctx -> {
        ctx.attribute("errors", Map.of());
        ctx.attribute("url", Map.of());
        ctx.render("urls/new.html");
    };

    public static Handler createUrl = ctx -> {
        String urlValue = ctx.formParam("url");


        Validator<String> urlValidator = ctx.formParamAsClass("url", String.class)
                // Добавляем проверку, что имя не должно быть пустым
                .check(it -> !it.isEmpty(), "url не должно быть пустым");

        Map<String, List<ValidationError<? extends Object>>> errors = JavalinValidation.collectErrors(
                urlValidator
        );

        if (!errors.isEmpty()) {
            // Устанавливаем код ответа
            ctx.status(422);
            // Передаем ошибки и данные компании
            ctx.attribute("errors", errors);
            Url invalidUrl = new Url(urlValue);
            ctx.attribute("url", invalidUrl);
            ctx.render("urls/new.html");
            return;
        }

        try {
            URL urlParser = new URL(urlValue);
            String newName = urlParser.getProtocol() + "://"  + urlParser.getAuthority();

            Url existedUrl = new QUrl()
                    .name.equalTo(newName)
                    .findOne();

            if (null != existedUrl) {
                ctx.sessionAttribute("flash", "Url already existed!");
                ctx.redirect("/urls");
                return;
            }

            Url url = new Url(newName);
            url.save();
        } catch (Exception exception) {
            ctx.sessionAttribute("flash", "Incorrect URL");
            ctx.redirect("/urls");
            return;
        }

        ctx.sessionAttribute("flash", "Url added successfully");
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

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };
}
