package com.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IntegrationTest extends ServletTest {
    private Runtime runtime;
    private ResourceRouter resourceRouter;
    private ResourceContext resourceContext;

    private Providers providers;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        resourceRouter = new DefaultResourceRouter(runtime, List.of(new ResourceHandler(UsersApi.class)));
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getResourceRouter()).thenReturn(resourceRouter);
        when(runtime.getProviders()).thenReturn(providers);
        return new ResourceServlet(runtime);
    }

    @BeforeEach
    void setUp() {
        RuntimeDelegate runtimeDelegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(runtimeDelegate);
        when(runtimeDelegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        when(runtimeDelegate.createHeaderDelegate(NewCookie.class)).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            @Override
            public String toString(NewCookie value) {
                return value.getName() + "=" + value.getValue();
            }
        });
    }

    @Test
    void should_return_404_if_url_inexist() {
        HttpResponse<String> response = get("/customers");
        assertEquals(404, response.statusCode());
    }
}


record UserData(String name, String email) {

}


class User {
    String id;
    UserData userData;

    public User(String id, UserData userData) {
        this.id = id;
        this.userData = userData;
    }

    public String getId() {
        return id;
    }

    public UserData getUserData() {
        return userData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

@Path("/users")
class UsersApi {

    List<User> users;

    public UsersApi() {
        this.users = List.of(new User("john-smith", new UserData("john-smith", "john-smith@email.com")));
    }

    @Path("{id}")
    public UserApi findUserById(@PathParam("id") String id) {
        return new UserApi(users.stream().filter(user -> user.getId().equals(id))
                .findFirst().orElseThrow(() -> new WebApplicationException("404")));
    }
}

class UserApi {
    private User user;

    public UserApi(User user) {
        this.user = user;
    }

    @GET
    public String get() {
        return "";
    }
}
