/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.errorhandling;

import at.nieslony.openvpnadmin.exceptions.PermissionDenied;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author claas
 */
public class ExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger LOG = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private final javax.faces.context.ExceptionHandler wrapped;

    public ExceptionHandler(final javax.faces.context.ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public javax.faces.context.ExceptionHandler getWrapped() {
        return this.wrapped;
    }

    @Override
    public void handle() {
               final Iterator<ExceptionQueuedEvent> queue = getUnhandledExceptionQueuedEvents().iterator();

        while (queue.hasNext()){
            ExceptionQueuedEvent item = queue.next();
            ExceptionQueuedEventContext exceptionQueuedEventContext = (ExceptionQueuedEventContext)item.getSource();

            try {
                Throwable throwable = exceptionQueuedEventContext.getException();
                Throwable rootCause = getRootCause(throwable);

                LOG.severe("--- Caught exception ---");
                LOG.severe(throwable.getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                LOG.severe(sw.toString());
                LOG.severe("--- End of exception ---");

                FacesContext context = FacesContext.getCurrentInstance();
                ExternalContext extContext = context.getExternalContext();
                Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
                HttpServletRequest request = (HttpServletRequest) extContext.getRequest();

                String errorMsg = "Unhandled Exception";
                String errorPage = "/error/error.xhtml";
                boolean isFatal = true;

                if (rootCause instanceof PermissionDenied) {
                    String message = String.format(
                            "Permission denied: \nPath: %s\nRemote IP: %s",
                            request.getRequestURL(),
                            request.getRemoteAddr());

                    LOG.warning(message);
                    errorMsg = "Access denied";
                    extContext.setResponseStatus(403);
                    isFatal = false;
                }
                else if (rootCause instanceof ELException) {
                    isFatal = true;
                }
                if (rootCause instanceof ViewExpiredException) {
                    LOG.warning("View expired");
                    errorPage = "Login.xhtml";
                    isFatal = false;
                }
                else {
                    extContext.setResponseStatus(500);
                }
                requestMap.put("errorMsg", errorMsg);

                if (!isFatal) {
                    try {
                        context.responseComplete();
                        LOG.info(String.format("Going to %s", errorPage));
                        try {
                            extContext.dispatch(errorPage);
                        }
                        catch (IOException ex) {
                            LOG.severe(String.format("Cannot go to error page: %s",
                            ex.getMessage()));
                        }
                        /*final ConfigurableNavigationHandler nav =
                                (ConfigurableNavigationHandler)
                                context.getApplication().getNavigationHandler();
                        nav.performNavigation(errPage);
                        context.renderResponse();*/
                    } catch (FacesException e) {
                        LOG.severe(String.format("Cannot dispatch error page: %s", e.getMessage()));
                    }
                    context.renderResponse();
                }
                else {
                    context.responseComplete();
                    LOG.info(String.format("Going to %s", errorPage));
                    try {
                        extContext.dispatch("/error/error.html");
                    }
                    catch (IOException ex) {
                        LOG.severe(String.format("Cannot go to error page: %s",
                        ex.getMessage()));
                    }
                    LOG.severe("#####  BEGIN ...---... #####");
                    LOG.severe(rootCause.toString());
                    LOG.severe("#####  ...---... END #####");
                }
            } finally {
                queue.remove();
            }
        }
    }
}
