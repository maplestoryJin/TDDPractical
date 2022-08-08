package com.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceDispatcherTest {
    private RuntimeDelegate delegate;

    private Runtime runtime;
    private HttpServletRequest request;
    private ResourceContext context;
    private UriInfoBuilder builder;

    @BeforeEach
    void before() {
        runtime = mock(Runtime.class);
        delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        request = mock(HttpServletRequest.class);
        context = mock(ResourceContext.class);
        when(request.getServletPath()).thenReturn("/users/1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaders(eq(HttpHeaders.ACCEPT))).thenReturn(new Vector<>(List.of(MediaType.WILDCARD)).elements());
        builder = mock(UriInfoBuilder.class);
        when(runtime.createUriInfoBuilder(eq(request))).thenReturn(builder);

    }

    // TODO: 根据与Path匹配结果， 降序排列RootResource, 选择第一个的RootResource
    // TODO: R1, R2, R1 matched, R2 none R1
    // TODO: R1, R2, R1, R2, matched, R1 result < R2 result R1

    @Test
    void should_use_matched_root_resource() {
        GenericEntity entity = new GenericEntity("matched", String.class);

        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(
                rootResource(matched("/users/1", result("/1")), returns(entity)),
                rootResource(unMatched("/users/1"))));
        OutBoundResponse response = router.dispatch(request, context);
        assertSame(entity, response.getGenericEntity());
        assertEquals(200, response.getStatus());
    }

    @Test
    void should_sort_matched_root_resource_descending_order() {
        GenericEntity entity1 = new GenericEntity("1", String.class);
        GenericEntity entity2 = new GenericEntity("2", String.class);

        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(
                rootResource(matched("/users/1", result("/1", 2)), returns(entity2)),
                rootResource(matched("/users/1", result("/1", 1)), returns(entity1))
        ));

        OutBoundResponse response = router.dispatch(request, context);
        assertSame(entity1, response.getGenericEntity());
        assertEquals(200, response.getStatus());

    }

    private ResourceRouter.RootResource rootResource(UriTemplate unMatchedUriTemplate) {
        ResourceRouter.RootResource unMatched = mock(ResourceRouter.RootResource.class);
        when(unMatched.getUriTemplate()).thenReturn(unMatchedUriTemplate);
        return unMatched;
    }

    private UriTemplate unMatched(String path) {
        UriTemplate unMatchedUriTemplate = mock(UriTemplate.class);
        when(unMatchedUriTemplate.match(path)).thenReturn(Optional.empty());
        return unMatchedUriTemplate;
    }

    private ResourceRouter.RootResource rootResource(UriTemplate matchedUriTemplate, ResourceRouter.ResourceMethod method) {
        ResourceRouter.RootResource matched = mock(ResourceRouter.RootResource.class);
        when(matched.getUriTemplate()).thenReturn(matchedUriTemplate);
        when(matched.match(eq("/1"), eq("GET"), eq(new String[]{MediaType.WILDCARD}), eq(builder))).thenReturn(Optional.of(method));
        return matched;
    }

    private ResourceRouter.ResourceMethod returns(GenericEntity entity) {
        ResourceRouter.ResourceMethod method = mock(ResourceRouter.ResourceMethod.class);
        when(method.call(eq(context), eq(builder))).thenReturn(entity);
        return method;
    }

    private UriTemplate matched(String path, UriTemplate.MatchResult result) {
        UriTemplate matchedUriTemplate = mock(UriTemplate.class);
        when(matchedUriTemplate.match(eq(path))).thenReturn(Optional.of(result));
        return matchedUriTemplate;
    }

    private UriTemplate.MatchResult result(String path) {
        return new FakeMatchedResult(path, 0);
    }


    private UriTemplate.MatchResult result(String path, int order) {
        return new FakeMatchedResult(path, order);
    }

    private class FakeMatchedResult implements UriTemplate.MatchResult {
        private String remaining;
        private Integer order;

        private FakeMatchedResult(String remaining, Integer order) {
            this.remaining = remaining;
            this.order = order;
        }

        @Override
        public String getMatched() {
            return null;
        }

        @Override
        public String getRemaining() {
            return remaining;
        }

        @Override
        public Map<String, String> getMatchedPathParameters() {
            return null;
        }

        @Override
        public int compareTo(UriTemplate.MatchResult o) {
            return order.compareTo(((FakeMatchedResult) o).order);
        }
    }



    // TODO: 如果没有匹配的RootResource, 则构造404的Response
    // TODO: 如果返回的RootResource中无法匹配剩余Path, 则构造404的Response
    // TODO: 如果ResourceMethod返回null, 则构造204的Response
}
