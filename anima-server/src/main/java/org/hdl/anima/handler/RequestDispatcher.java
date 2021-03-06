package org.hdl.anima.handler;

import org.hdl.anima.Application;
import org.hdl.anima.common.module.BasicModule;
import org.hdl.anima.protocol.Request;
import org.hdl.anima.protocol.Response;
import org.hdl.anima.session.ISession;

/**
 * Client request dispatcher
 * @author qiuhd
 * @since  2014年9月23日
 * @version V1.0.0
 */
public class RequestDispatcher extends BasicModule {
	
//	private static final Logger logger = LoggerFactory.getLogger(RequestDispather.class); 
	private RequestMappingMethodHandler handerMapping;
	private HandlerAdapter handelrAdapter ;
	
	public RequestDispatcher(String moduleName) {
		super(moduleName);
	}
	
	@Override
	public void initialize(Application application) {
		super.initialize(application);
		this.handerMapping = application.getMoulde(RequestMappingMethodHandler.class);
		this.handelrAdapter = new MethodHandlerAapater();
	}

	/**
	 * Request dispatch
	 * @param requset
	 * @param session
	 */
	public void dispatch(Request request,ISession session) {
		
		if (request.isBroken()) {
			handleBadRequest(request,session);
			return ;
		}
		
		HandlerExecutionChain handlerChain = handerMapping.getHandler(request);
		
		if (handlerChain == null) {
			handleUnknownRequest(request,session);
			return ;
		}
		
		try {
			if(!handlerChain.applyPreHandle(request, session)) {
				return ;
			}
			
			Object result = handelrAdapter.handle(request, session, handlerChain.getHandler());
			
			handlerChain.applyPostHandle(request, session, result);
		} catch (Exception e) {
			if (request.isRequest()) {
				Response response = new Response(request.getId(),Response.SERVICE_ERROR);
				response.setSequence(request.getSequence());
				response.setSid(request.getSid());
				response.setErrorDes("Service Error:" + e.getMessage());
				response.setContent(null);
				session.send(response);
				throw new IllegalStateException("Failed to handle request,Reuqest info:" + request,e);
			}else {
				throw new IllegalStateException("Failed to handle request,Reuqest info:" + request,e);
			}
		}
	}
	
	/**
	 * Handle not found request mapping
	 * @param request
	 * @param session
	 */
	private void handleUnknownRequest(Request request,ISession session) {
		if (!request.isNotify()) {
			Response response = new Response(request.getId(),Response.SERVICE_NOT_FOUND,"Service Unfound!");
			response.setSequence(request.getSequence());
			session.send(response);
		}
		throw new IllegalStateException("No found request handler in request mapping,Request info :" + request);
	}
	
	/**
	 * Handle bad request
	 * @param request
	 * @param session
	 */
	private void handleBadRequest(Request request,ISession session) {
		if (!request.isNotify()) {
			Response response = new Response(request.getId(),Response.BAD,"Bad Request!");
			response.setSequence(request.getSequence());
			session.send(response);
		}
		
		if (request.getContent() instanceof Throwable) {
			Throwable t = (Throwable) request.getContent();
			throw new IllegalStateException("Bad request,Request info :" + request, t);
		}else {
			throw new IllegalStateException("Bad request,Request info :" + request);
		}
	}
}
