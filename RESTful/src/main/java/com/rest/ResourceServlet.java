package com.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;

public class ResourceServlet extends HttpServlet {

    private Runtime runtime;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();
        OutBoundResponse response;
        try {
            response = router.dispatch(req, runtime.createResourceContext(req, resp));
        } catch (WebApplicationException exception) {
            response = (OutBoundResponse) exception.getResponse();
        } catch (Throwable throwable) {
            ExceptionMapper exceptionMapper = runtime.getProviders().getExceptionMapper(throwable.getClass());
            response = (OutBoundResponse) exceptionMapper.toResponse(throwable);
        }
        resp.setStatus(response.getStatus());
        for (String name : response.getHeaders().keySet()) {
            for (Object value : response.getHeaders().get(name)) {
                RuntimeDelegate.HeaderDelegate headerDelegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
                resp.addHeader(name, headerDelegate.toString(value));
            }
        }

        Providers providers = runtime.getProviders();
        GenericEntity entity = response.getGenericEntity();
        MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
        writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(), response.getHeaders(), resp.getOutputStream());
    }
}
