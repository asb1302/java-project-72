package hexlet.code.service;

import hexlet.code.domain.UrlCheckResponse;

public interface UrlCheckServiceInterface {
    public UrlCheckResponse check(String url);
}
