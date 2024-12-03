package com.hoit.checkers.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.hoit.checkers.model.User;

import java.util.Map;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

	@Override
	public boolean beforeHandshake(ServerHttpRequest request,
	                               ServerHttpResponse response,
	                               WebSocketHandler wsHandler,
	                               Map<String, Object> attributes) throws Exception {
	    if (request instanceof ServletServerHttpRequest) {
	        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
	        HttpSession session = servletRequest.getServletRequest().getSession(false);

	        if (session == null) {
	            System.out.println("Session is null. This should not happen after login.");
	            // 이 경우 세션이 null로 나오는 원인을 반드시 찾아야 함.
	            return false; // 세션이 없는 경우 핸드셰이크를 거부하는 것이 더 안전할 수 있음
	        }

	        // 세션이 존재할 경우 사용자 정보 설정
	        User user = (User) session.getAttribute("user");
	        if (user != null) {
	            attributes.put("user", user);
	            attributes.put("sessionId", session.getId());
	            System.out.println("User added to attributes: " + user.getNickname());
	        } else {
	            System.out.println("User is not present in the session.");
	        }
	    }
	    return true;
	}

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No action needed after handshake
    }
}
