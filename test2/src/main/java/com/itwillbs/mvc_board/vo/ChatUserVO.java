package com.itwillbs.mvc_board.vo;

import org.springframework.web.socket.WebSocketSession;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatUserVO {
	private String sessionId;
	private WebSocketSession session;
}
