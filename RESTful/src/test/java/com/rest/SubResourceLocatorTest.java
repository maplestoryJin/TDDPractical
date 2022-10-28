package com.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubResourceLocatorTest extends InjectableCallerTest {

    private UriTemplate.MatchResult result;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        result = Mockito.mock(UriTemplate.MatchResult.class);
    }

    @Override
    protected void callInjectable(String method, Class<?> type) throws NoSuchMethodException {
        SubResourceLocators.SubResourceLocator subResourceLocator = new SubResourceLocators.SubResourceLocator(SubResourceMethods.class.getMethod(method, type));
        subResourceLocator.match(result, "GET", new String[0], resourceContext, builder);
    }

    @Override
    protected Object initResource() {
        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SubResourceMethods.class}, (proxy, method, args) -> {
            lastCall = new LastCall(getMethodName(method.getName(), Arrays.stream(method.getParameters()).map(p -> p.getType()).collect(Collectors.toList())),
                    args != null ? List.of(args) : List.of());
            return new Message();
        });
    }

    interface SubResourceMethods {
        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") String path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") double path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") float path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") short path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") boolean path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") byte path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") char path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") int path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") Converter path);

        @Path("/message/{param}")
        Message getPathParam(@PathParam("param") BigDecimal path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") String path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") double path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") float path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") short path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") boolean path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") byte path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") char path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") int path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") Converter path);

        @Path("/message/")
        Message getQueryParam(@QueryParam("param") BigDecimal path);

        @Path("/message")
        Message getContext(@Context SomeServiceInContext service);

        @Path("/message")
        Message getContext(@Context ResourceContext context);

        @Path("/message")
        Message getContext(@Context UriInfo uriInfo);
    }

    static class Message {

        @GET
        public String content() {
            return "content";
        }
    }
}
