package com.itwillbs.mvc_board.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.itwillbs.mvc_board.service.BankService;
import com.itwillbs.mvc_board.vo.ResponseTokenVO;
import com.itwillbs.mvc_board.vo.ResponseUserInfoVO;

@Controller
public class FintechController {
	@Autowired
	private BankService bankService;
	
	// 로그 출력을 위한 기본 라이브러리(org.slf4j.Logger 타입) 변수 선언
	// => org.slf4j.LoggerFactory.getLogger() 메서드 호출하여 Logger 객체 리턴받아 사용 가능
	//    파라미터 : 로그를 사용하여 다룰 현재 클래스 지정(해당 클래스에서 발생한 로그로 처리)
	private static final Logger logger = LoggerFactory.getLogger(FintechController.class);
	// => Logger 객체의 다양한 로그 출력 메서드(info, debug, warn, error 등) 활용하여 로그 출력 가능
	//    (각 메서드는 로그의 심각도(레벨)에 따라 구별하는 용도로 사용)
	// -------------------------------------------------------------------------
	// "/FintechMain" 매핑 => fintech/main.jsp 페이지 포워딩
	@GetMapping("FintechMain")
	public String fintechMain(HttpSession session, Model model) {
		if(session.getAttribute("sId") == null) {
			model.addAttribute("msg", "로그인 필수!");
			model.addAttribute("targetURL", "MemberLoginForm");
			return "forward";
		}
		
		// 랜덤값을 활용하여 32바이트 난수 생성 후 세션에 저장 후 메인페이지로 포워딩 
		String rNum = RandomStringUtils.randomNumeric(32);
//		logger.info("난수 : " + rNum);
		
		session.setAttribute("state", rNum);
		
		return "fintech/main";
	}
	
	@GetMapping("callback")
	public String callback(@RequestParam Map<String, String> authResponse, HttpSession session, Model model) {
		// 콜백을 통해 전달되는 응답 데이터 3가지(code, scope, client_info) 파라미터값이
		// Map 객체에 자동으로 저장됨
		logger.info("authResponse : " + authResponse.toString());
		
		// ----------------------------------------------------
		String id = (String)session.getAttribute("sId");
		if(id == null) {
			// "fail_back.jsp" 페이지로 포워딩 시 "isClose" 값을 true 로 설정하여 전달
			model.addAttribute("msg", "로그인 필수!");
			model.addAttribute("isClose", true); // 현재 창(서브 윈도우) 닫도록 명령
			return "fail_back";
		} 

		System.out.println("에러메세지 : " + authResponse.get("error"));
		
		// ----------------------------------------------------
		// 응답 데이터 중 state 값이 요청 시 사용된 값인지 판별
		System.out.println("state 삭제 전 : " + session.getAttribute("state"));
		if(session.getAttribute("state") == null || !session.getAttribute("state").equals(authResponse.get("state"))) { // 세션의 state 값과 응답 데이터의 state 값이 다를 경우
			// "잘못된 요청입니다!" 출력 후 이전페이지로 돌아가기
			model.addAttribute("msg", "잘못된 요청입니다!");
			return "fail_back";
		}
		
		// 확인 완료된 세션의 state 값 삭제
		session.removeAttribute("state"); // invalidate() 메서드 호출 아님!!!!
		System.out.println("state 삭제 후 : " + session.getAttribute("state"));
		// ----------------------------------------------------
		// 2.1.2. 토큰발급 API - 사용자 토큰 발급 API 요청
		// BankApiService - requestAccessToken() 메서드 호출
		// => 파라미터 : 토큰 발급 요청에 필요한 정보(인증코드 요청 결과 Map 객체)
		//    리턴타입 : ResponseTokenVO(responseToken)
		ResponseTokenVO responseToken = bankService.requestAccessToken(authResponse);
		logger.info("엑세스토큰 : " + responseToken);
		
		// ResponseTokenVO 객체가 null 이거나 엑세스토큰 값이 null 일 경우 에러 처리
		// "forward.jsp" 페이지로 포워딩 시 "isClose" 값을 true 로 설정하여 전달
		// => state 값 갱신 위해 "FintechMain" 서블릿 주소 설정
		if(responseToken == null || responseToken.getAccess_token() == null) {
			model.addAttribute("msg", "토큰 발급 실패! 다시 인증하세요!");
			model.addAttribute("isClose", true);
			model.addAttribute("targetURL", "FintechMain");
			return "forward";
		}
		
		// BankApiService - registAccessToken() 메서드 호출하여 토큰 관련 정보 저장 요청
		// => 파라미터 : 세션 아이디, ResponseTokenVO 객체
		// => 만약, 하나의 객체로 전달할 경우(Map 객체 활용)
//		bankApiService.registAccessToken(id, responseToken);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", id);
		map.put("token", responseToken);
		bankService.registAccessToken(map);
		
		// 세션 객체에 엑세스 토큰(access_token), 사용자번호(user_seq_no) 저장
		session.setAttribute("access_token", responseToken.getAccess_token());
		session.setAttribute("user_seq_no", responseToken.getUser_seq_no());
		
		// "forward.jsp" 페이지 포워딩을 통해
		// "계좌 인증 완료" 메세지 출력 후 인증 창 닫고 FintechUserInfo 서블릿 요청
		model.addAttribute("msg", "계좌 인증 완료!");
		model.addAttribute("isClose", true);
		model.addAttribute("targetURL", "FintechUserInfo");
		
		return "forward";
	}
	
	// 2.2.1. 사용자정보조회 API
	@GetMapping("FintechUserInfo")
	public String requestUserInfo(HttpSession session, Model model) {
		// 세션아이디가 null 일 경우 로그인 페이지 이동 처리
		// 엑세스토큰이 null 일 경우 "계좌 인증 필수!" 메세지 출력 후 "forward.jsp" 페이지 포워딩
		if(session.getAttribute("sId") == null) {
			model.addAttribute("msg", "로그인 필수!");
//			model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "MemberLoginForm");
			return "forward";
		} else if(session.getAttribute("access_token") == null) {
			model.addAttribute("msg", "계좌 인증 필수!");
//			model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "FintechMain");
			return "forward";
		}
		
		// Map 객체에 세션에 저장된 엑세스 토큰(access_token)과 사용자번호(user_seq_no) 저장
		Map<String, String> map = new HashMap<String, String>();
		map.put("access_token", (String)session.getAttribute("access_token"));
		map.put("user_seq_no", (String)session.getAttribute("user_seq_no"));
		// => Map 객체의 제네릭 타입을 String, Object 로 사용해도 무관(세션값 형변환 불필요)
		
		// 2.2. 사용자/서비스 관리 - 2.2.1. 사용자정보조회 API 요청
		// BankService - requestUserInfo() 메서드 호출하여 핀테크 사용자 정보조회 요청
		// => 파라미터 : Map 객체   리턴타입 : ResponseUserInfoVO(userInfo)
//		ResponseUserInfoVO userInfo = bankService.requestUserInfo(map);
		
		// 만약, 응답데이터를 Map 타입으로 처리할 경우(Map<String, Object> 타입 사용)
		Map<String, Object> userInfo = bankService.requestUserInfo(map);
		logger.info(">>>>>> userInfo : " + userInfo);
		
		// Model 객체에 ResponseUserInfo 객체 저장
		model.addAttribute("userInfo", userInfo);
		
		return "fintech/fintech_user_info";
	}
	
	// 2.3.1. 잔액조회 API
	@PostMapping("BankAccountDetail")
	public String accountDetail(@RequestParam Map<String, String> map, HttpSession session, Model model) {
		// 세션아이디가 null 일 경우 로그인 페이지 이동 처리
		// 엑세스토큰이 null 일 경우 "계좌 인증 필수!" 메세지 출력 후 "forward.jsp" 페이지 포워딩
		if(session.getAttribute("sId") == null) {
			model.addAttribute("msg", "로그인 필수!");
//			model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "MemberLoginForm");
			return "forward";
		} else if(session.getAttribute("access_token") == null) {
			model.addAttribute("msg", "계좌 인증 필수!");
//			model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "FintechMain");
			return "forward";
		}
		
		// 요청에 사용할 엑세스토큰(세션)을 Map 객체에 추가
		map.put("access_token", (String)session.getAttribute("access_token"));
		
		// BankService - requestAccountDetail() 메서드 호출하여 계좌 상세정보 조회 요청
		// => 파라미터 : Map 객체   리턴타입 : Map<String, Object>(accountDetail)
		Map<String, Object> accountDetail = bankService.requestAccountDetail(map);
		
		// 조회결과(Map 객체, 이름, 계좌번호) 저장
		model.addAttribute("accountDetail", accountDetail);
		model.addAttribute("user_name", map.get("user_name"));
		model.addAttribute("account_num_masked", map.get("account_num_masked"));
		
		return "fintech/fintech_account_detail";
	}
	
	// 2.5.1. 출금이체 API
	@PostMapping("BankPayment")
	public String bankPayment(@RequestParam Map<String, String> map, HttpSession session, Model model) {
//		logger.info(">>>>> payment : " + map);
		
		String id = (String)session.getAttribute("sId");
		// 세션아이디가 null 일 경우 로그인 페이지 이동 처리
		// 엑세스토큰이 null 일 경우 "계좌 인증 필수!" 메세지 출력 후 "forward.jsp" 페이지 포워딩
		if(id == null) {
			model.addAttribute("msg", "로그인 필수!");
//					model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "MemberLoginForm");
			return "forward";
		} else if(session.getAttribute("access_token") == null) {
			model.addAttribute("msg", "계좌 인증 필수!");
//					model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "FintechMain");
			return "forward";
		}
		
		// 요청에 필요한 엑세스토큰과 세션 아이디를 Map 객체에 추가
		map.put("access_token", (String)session.getAttribute("access_token"));
		map.put("id", id);
		
		// BankService - requestWithdraw() 메서드 호출하여 상품 구매에 대한 지불(출금이체) 요청
		// => 파라미터 : Map 객체   리턴타입 : Map<String, Object>(withdrawResult)
		Map<String, Object> withdrawResult = bankService.requestWithdraw(map);
		logger.info(">>>>>>> withdrawResult : " + withdrawResult);
		
		// 요청 결과를 model 객체에 저장
		model.addAttribute("withdrawResult", withdrawResult);
		
		return "fintech/fintech_payment_result";
	}
	
	// =============================================================================
	// 입금이체는 이용기관(ex. 아이티윌)의 계좌에서 사용자의 계좌로 자금이 이동하므로
	// 이용기관의 계좌에 접근 가능한 엑세스토큰 필요하다.
	// 또한, 입금이체를 위해서는 별도로 oob 권한이 있는 엑세스토큰 발급 필요하다.
	// 2.1.2. 토큰발급 API - 센터인증 이용기관 토큰발급 API (2-legged)
	@GetMapping("FintechAdminAccessToken")
	public String bankAdminAccessToken(HttpSession session, Model model) {
		String id = (String)session.getAttribute("sId");
		// 세션아이디가 null 일 경우 로그인 페이지 이동 처리
		// 관리자가 아닐 경우 "잘못된 접근입니다!" 처리
		if(id == null) {
			model.addAttribute("msg", "로그인 필수!");
			model.addAttribute("targetURL", "MemberLoginForm");
			return "forward";
		} else if(!id.equals("admin")) {
			model.addAttribute("msg", "잘못된 접근입니다!");
			model.addAttribute("targetURL", "./");
			return "forward";
		} 
		
		// BankService - requestAdminAccessToken() 메서드 호출하여 관리자 엑세스토큰 발급 요청
		// => 파라미터 : 없음   리턴타입 : Map<String, Object>(responseToken)
		ResponseTokenVO responseToken = bankService.requestAdminAccessToken();
		System.out.println(">>>>>>> 관리자 엑세스토큰 : " + responseToken);
		
		// refresh_token 과 user_seq_no 값은 널스트링("")으로 설정
		responseToken.setRefresh_token("");
		responseToken.setUser_seq_no("");
		
		// BankApiService - registAccessToken() 메서드 호출하여 토큰 관련 정보 저장 요청
		// => 파라미터 : 세션 아이디, ResponseTokenVO 객체
		// => 만약, 하나의 객체로 전달할 경우(Map 객체 활용)
//				bankApiService.registAccessToken(id, responseToken);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", id);
		map.put("token", responseToken);
		bankService.registAccessToken(map);
		
		model.addAttribute("msg", "토큰 발급 완료!");
		model.addAttribute("targetURL", "FintechMain");
		return "forward";
	}
	// =============================================================================
	
	// 2.5.2. 입금이체 API
	@PostMapping("BankRefund")
	public String bankRefund(@RequestParam Map<String, String> map, HttpSession session, Model model) {
//		logger.info(">>>>> payment : " + map);
		
		String id = (String)session.getAttribute("sId");
		// 세션아이디가 null 일 경우 로그인 페이지 이동 처리
		// 엑세스토큰이 null 일 경우 "계좌 인증 필수!" 메세지 출력 후 "forward.jsp" 페이지 포워딩
		if(id == null) {
			model.addAttribute("msg", "로그인 필수!");
//					model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "MemberLoginForm");
			return "forward";
		} else if(session.getAttribute("access_token") == null) {
			model.addAttribute("msg", "계좌 인증 필수!");
//					model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "FintechMain");
			return "forward";
		}
		
//		map.put("access_token", (String)session.getAttribute("access_token"));
		// 요청에 필요한 엑세스토큰과 세션 아이디를 Map 객체에 추가
		// => 주의! 입금이체에 필요한 엑세스토큰은 이용기관의 엑세스토큰이므로
		//    저장된 "admin" 계정의 엑세스토큰(oob) 조회 필요
		// => BankService - getAdminAccessToken() 메서드 호출하여 관리자 엑세스토큰 조회
		map.put("access_token", bankService.getAdminAccessToken());
		map.put("id", id);
		System.out.println(">>>>>> 입금이체 map 데이터 : " + map);
		
		// BankService - requestDeposit() 메서드 호출하여 상품에 대한 환불(입금이체) 요청
		// => 파라미터 : Map 객체   리턴타입 : Map<String, Object>(depositResult)
		Map<String, Object> depositResult = bankService.requestDeposit(map);
		logger.info(">>>>>>> depositResult : " + depositResult);
//		
		// 요청 결과를 model 객체에 저장
		model.addAttribute("depositResult", depositResult);
		
		return "fintech/fintech_refund_result";
	}
	
	// 송금(2.5.1 출금이제 API + 2.5.2. 입금이체 API)
	@PostMapping("BankTransfer")
	public String bankTransfer(@RequestParam Map<String, String> map, HttpSession session, Model model) {
		String id = (String)session.getAttribute("sId");
		// 세션아이디가 null 일 경우 로그인 페이지 이동 처리
		// 엑세스토큰이 null 일 경우 "계좌 인증 필수!" 메세지 출력 후 "forward.jsp" 페이지 포워딩
		if(id == null) {
			model.addAttribute("msg", "로그인 필수!");
//					model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "MemberLoginForm");
			return "forward";
		} else if(session.getAttribute("access_token") == null) {
			model.addAttribute("msg", "계좌 인증 필수!");
//					model.addAttribute("isClose", true); // 새 창이 아니므로 창 닫기 불필요
			model.addAttribute("targetURL", "FintechMain");
			return "forward";
		}
		
		map.put("access_token", (String)session.getAttribute("access_token"));
		map.put("id", id);
		map.put("admin_access_token", bankService.getAdminAccessToken());
		System.out.println(">>>>>> 송금 map 데이터 : " + map);
		
		// BankService - requestTransfer() 메서드 호출하여 상품에 대한 환불(입금이체) 요청
		// => 파라미터 : Map 객체   리턴타입 : Map<String, Object>(transferResult)
		Map<String, Object> transferResult = bankService.requestTransfer(map);
		logger.info(">>>>>>> transferResult : " + transferResult);
//		
		// 요청 결과를 model 객체에 저장
		model.addAttribute("transferResult", transferResult);
		
		return "fintech/fintech_transfer_result";
	}
	
	
}


























