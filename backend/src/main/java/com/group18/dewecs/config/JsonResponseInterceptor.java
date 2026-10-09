package com.group18.dewecs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group18.dewecs.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * When an officer-page client asks for JSON, the controller still runs unchanged and this interceptor swaps what it
 * would have rendered:
 * <ul>
 *   <li>a page (view name plus model): the model as a JSON object, without the form beans and binding results;</li>
 *   <li>a redirect after an action: {@code {message, location}} (201 for a create, else 200), or a 400 problem when
 *       the controller set a rule-violation {@code error};</li>
 *   <li>a re-shown form with binding errors: a 400 problem with {@code fieldErrors}.</li>
 * </ul>
 * Errors thrown by the controllers (404, unexpected) are already JSON through GlobalExceptionHandler.
 */
public class JsonResponseInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public JsonResponseInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView == null || !JsonNegotiation.wantsJson(request)) {
            return;
        }
        String viewName = modelAndView.getViewName();
        if (viewName != null && viewName.startsWith("redirect:")) {
            modelAndView.setView(actionResult(request, viewName.substring("redirect:".length())));
        } else if (hasBindingErrors(modelAndView.getModel())) {
            modelAndView.setView(validationProblem(request, modelAndView.getModel()));
        } else {
            modelAndView.setView(new JsonView(HttpStatus.OK, MediaType.APPLICATION_JSON, publicModel(modelAndView.getModel())));
        }
        modelAndView.getModel().clear();
    }

    private View actionResult(HttpServletRequest request, String location) {
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        Object error = flash == null ? null : flash.get("error");
        Object message = flash == null ? null : flash.get("message");
        if (error != null) {
            return problem(request, HttpStatus.BAD_REQUEST, String.valueOf(error), Map.of());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("location", location);
        boolean created = "POST".equals(request.getMethod()) && isCollectionRoot(request);
        return new JsonView(created ? HttpStatus.CREATED : HttpStatus.OK, MediaType.APPLICATION_JSON, body, location);
    }

    /** POST /shelters, /warnings, ... (one path segment) creates a record. */
    private boolean isCollectionRoot(HttpServletRequest request) {
        String path = request.getRequestURI().replaceAll("/+$", "");
        return path.indexOf('/', 1) < 0;
    }

    private boolean hasBindingErrors(Map<String, Object> model) {
        return model.values().stream().anyMatch(v -> v instanceof BindingResult b && b.hasErrors());
    }

    private View validationProblem(HttpServletRequest request, Map<String, Object> model) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        String detail = null;
        for (Object value : model.values()) {
            if (value instanceof BindingResult result && result.hasErrors()) {
                for (FieldError error : result.getFieldErrors()) {
                    fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
                }
                for (ObjectError error : result.getGlobalErrors()) {
                    if (detail == null) {
                        detail = error.getDefaultMessage();
                    }
                }
            }
        }
        return problem(request, HttpStatus.BAD_REQUEST, detail != null ? detail : "Validation failed", fieldErrors);
    }

    private View problem(HttpServletRequest request, HttpStatus status, String detail, Map<String, String> fieldErrors) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        if (!fieldErrors.isEmpty()) {
            body.setProperty("fieldErrors", fieldErrors);
        }
        return new JsonView(status, MediaType.APPLICATION_PROBLEM_JSON, body);
    }

    /**
     * The model as a client should see it: no form beans or binding results, and staff or citizen users reduced to
     * id and name (the drop-downs never needed their phone numbers or addresses).
     */
    private Map<String, Object> publicModel(Map<String, Object> model) {
        Map<String, Object> out = new LinkedHashMap<>();
        model.forEach((key, value) -> {
            if (value instanceof BindingResult || key.equals("form") || key.endsWith("Form")) {
                return;
            }
            out.put(key, reduceUsers(value));
        });
        return out;
    }

    private Object reduceUsers(Object value) {
        if (value instanceof User user) {
            return userRef(user);
        }
        if (value instanceof Collection<?> items && items.stream().anyMatch(i -> i instanceof User)) {
            List<Object> reduced = new ArrayList<>();
            items.forEach(i -> reduced.add(i instanceof User u ? userRef(u) : i));
            return reduced;
        }
        return value;
    }

    private Map<String, Object> userRef(User user) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", user.getId());
        ref.put("fullName", user.getFullName());
        return ref;
    }

    /** Writes one JSON body with a fixed status and content type. */
    private final class JsonView implements View {

        private final HttpStatus status;
        private final MediaType type;
        private final Object body;
        private final String location;

        JsonView(HttpStatus status, MediaType type, Object body) {
            this(status, type, body, null);
        }

        JsonView(HttpStatus status, MediaType type, Object body, String location) {
            this.status = status;
            this.type = type;
            this.body = body;
            this.location = location;
        }

        @Override
        public String getContentType() {
            return type.toString();
        }

        @Override
        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
                throws Exception {
            response.setStatus(status.value());
            response.setContentType(type.toString());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            if (location != null && status == HttpStatus.CREATED) {
                response.setHeader("Location", location);
            }
            response.setHeader("Cache-Control", "no-store");
            objectMapper.writeValue(response.getOutputStream(), body);
        }
    }
}
