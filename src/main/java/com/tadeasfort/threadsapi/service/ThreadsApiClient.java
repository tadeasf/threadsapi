package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.config.ThreadsApiConfig;
import com.tadeasfort.threadsapi.config.ThreadsAppConfig;
import com.tadeasfort.threadsapi.dto.CreatePostRequest;
import com.tadeasfort.threadsapi.dto.ThreadsPostResponse;
import com.tadeasfort.threadsapi.dto.ThreadsTokenResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class ThreadsApiClient {

    private final ThreadsApiConfig threadsApiConfig;
    private final ThreadsAppConfig threadsAppConfig;
    private final RestTemplate restTemplate;

    public ThreadsApiClient(ThreadsApiConfig threadsApiConfig, ThreadsAppConfig threadsAppConfig) {
        this.threadsApiConfig = threadsApiConfig;
        this.threadsAppConfig = threadsAppConfig;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Exchange authorization code for access token
     */
    public ThreadsTokenResponse exchangeCodeForToken(String code, String redirectUri) {
        String url = threadsApiConfig.getTokenUrl();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", threadsAppConfig.getId());
        params.add("client_secret", threadsAppConfig.getSecret());
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<ThreadsTokenResponse> response = restTemplate.postForEntity(
                url, request, ThreadsTokenResponse.class);

        return response.getBody();
    }

    /**
     * Exchange short-lived token for long-lived token
     */
    public ThreadsTokenResponse exchangeForLongLivedToken(String shortLivedToken) {
        String url = UriComponentsBuilder.fromUriString(threadsApiConfig.getBaseUrl() + "/access_token")
                .queryParam("grant_type", "th_exchange_token")
                .queryParam("client_secret", threadsAppConfig.getSecret())
                .queryParam("access_token", shortLivedToken)
                .toUriString();

        ResponseEntity<ThreadsTokenResponse> response = restTemplate.getForEntity(url, ThreadsTokenResponse.class);
        return response.getBody();
    }

    /**
     * Create a new Threads post
     */
    public ThreadsPostResponse createPost(String accessToken, CreatePostRequest postRequest) {
        String url = threadsApiConfig.getBaseUrl() + "/me/threads";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(accessToken);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("media_type", postRequest.getMediaType());

        if (postRequest.getText() != null) {
            params.add("text", postRequest.getText());
        }
        if (postRequest.getImageUrl() != null) {
            params.add("image_url", postRequest.getImageUrl());
        }
        if (postRequest.getVideoUrl() != null) {
            params.add("video_url", postRequest.getVideoUrl());
        }
        if (postRequest.getIsCarouselItem() != null) {
            params.add("is_carousel_item", postRequest.getIsCarouselItem().toString());
        }
        if (postRequest.getChildren() != null && !postRequest.getChildren().isEmpty()) {
            params.add("children", String.join(",", postRequest.getChildren()));
        }
        if (postRequest.getReplyToId() != null) {
            params.add("reply_to_id", postRequest.getReplyToId());
        }
        if (postRequest.getQuotePostId() != null) {
            params.add("quote_post_id", postRequest.getQuotePostId());
        }
        if (postRequest.getLocationName() != null) {
            params.add("location_name", postRequest.getLocationName());
        }
        if (postRequest.getAltText() != null) {
            params.add("alt_text", postRequest.getAltText());
        }
        if (postRequest.getAllowCommenting() != null) {
            params.add("allow_commenting", postRequest.getAllowCommenting().toString());
        }
        if (postRequest.getHideLikeViewCounts() != null) {
            params.add("hide_like_view_counts", postRequest.getHideLikeViewCounts().toString());
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<ThreadsPostResponse> response = restTemplate.postForEntity(
                url, request, ThreadsPostResponse.class);

        return response.getBody();
    }

    /**
     * Publish a created post
     */
    public Map<String, Object> publishPost(String accessToken, String creationId) {
        String url = threadsApiConfig.getBaseUrl() + "/me/threads_publish";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(accessToken);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("creation_id", creationId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {
                });
        return response.getBody();
    }

    /**
     * Get user profile
     */
    public Map<String, Object> getUserProfile(String accessToken) {
        String url = UriComponentsBuilder.fromUriString(threadsApiConfig.getBaseUrl() + "/me")
                .queryParam("fields", "id,username,name,threads_profile_picture_url,threads_biography")
                .queryParam("access_token", accessToken)
                .toUriString();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {
                });
        return response.getBody();
    }

    /**
     * Get user's threads
     */
    public Map<String, Object> getUserThreads(String accessToken, String fields, Integer limit) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(threadsApiConfig.getBaseUrl() + "/me/threads")
                .queryParam("access_token", accessToken);

        if (fields != null) {
            builder.queryParam("fields", fields);
        }
        if (limit != null) {
            builder.queryParam("limit", limit);
        }

        String url = builder.toUriString();
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {
                });
        return response.getBody();
    }
}