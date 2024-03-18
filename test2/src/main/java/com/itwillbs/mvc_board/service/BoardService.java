package com.itwillbs.mvc_board.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itwillbs.mvc_board.mapper.BoardMapper;
import com.itwillbs.mvc_board.vo.BoardVO;

@Service
public class BoardService {
	@Autowired
	private BoardMapper mapper;

	// 게시물 등록 요청
	public int registBoard(BoardVO board) {
		return mapper.insertBoard(board);
	}

	// 게시물 목록 조회 요청
	public List<BoardVO> getBoardList(String searchType, String searchKeyword, int startRow, int listLimit) {
		return mapper.selectBoardList(searchType, searchKeyword, startRow, listLimit);
	}

	// 전체 게시물 목록 갯수 조회 요청
	public int getBoardListCount(String searchType, String searchKeyword) {
		return mapper.selectBoardListCount(searchType, searchKeyword);
	}

	// 게시물 상세정보 조회 요청
//	public BoardVO getBoard(int board_num) {
//		return mapper.selectBoard(board_num);
//	}
	// 단, 조회수 증가 여부를 컨트롤러에서 제어하기 위해 isIncreaseReadcount 파라미터 추가
	public BoardVO getBoard(int board_num, boolean isIncreaseReadcount) {
		BoardVO board = mapper.selectBoard(board_num);
		
		// 조회 결과가 존재하고 isIncreaseReadcount 가 true 일 경우 조회수 증가 작업 요청
		if(board != null && isIncreaseReadcount) {
			mapper.updateReadcount(board);
		}
		
		return board;
	}

	// 게시물 삭제 요청
	public int removeBoard(BoardVO board) {
		return mapper.deleteBoard(board);
	}

	// 게시물 수정 - 파일 삭제 요청
	public int removeBoardFile(BoardVO board) {
		return mapper.updateBoardFile(board);
	}

	// 게시물 수정 요청
	public int modifyBoard(BoardVO board) {
		return mapper.updateBoard(board);
	}

	// 답글 등록 요청
	// => 두 가지 DB 데이터 조작 작업을 위한 메서드 호출하는 과정에서
	//    트랜잭션을 적용하려면 복수개의 메서드를 호출하는 메서드 상단에
	//    @Transactional 어노테이션 적용(단, root-context.xml 내에 트랜잭션 설정 필수!)
	@Transactional
	public int registReplyBoard(BoardVO board) {
		// 기존 답글 순서번호 조정을 위해 updateBoardReSeq() 메서드 호출
		// => 파라미터 : BoardVO 객체   리턴타입 : void
		mapper.updateBoardReSeq(board);
		
		// 답글 등록 작업을 위해 insertReplyBoard() 메서드 호출
		// => 파라미터 : BoardVO 객체   리턴타입 : int
		return mapper.insertReplyBoard(board);
	}

	// 댓글 작성 요청
	public int registTinyReplyBoard(Map<String, String> map) {
		return mapper.insertTinyReplyBoard(map);
	}

	// 댓글 목록 조회 요청
	public List<Map<String, Object>> getTinyReplyBoardList(int board_num) {
		return mapper.selectTinyReplyBoardList(board_num);
	}

	// 댓글 작성자 조회 요청
	public Map<String, String> getTinyReplyWriter(Map<String, String> map) {
		return mapper.selectTinyReplyWriter(map);
	}

	// 댓글 삭제 요청
	public int removeTinyReplyBoard(Map<String, String> map) {
		return mapper.deleteTinyReplyBoard(map);
	}

	// 대댓글 등록 요청
	// => 단, 두 가지 이상의 작업을 수행해야할 경우 트랜잭션 처리
	@Transactional
	public int registTinyReReplyBoard(Map<String, String> map) {
		// 기존 댓글들의 순서 조정
		// => 단, 댓글들을 작성한 순서대로 나열할 경우(최신 댓글이 아래쪽에 위치할 경우) 불필요
//		mapper.updateTinyReplyBoardSeq(map);
		
		// 대댓글 등록
		return mapper.insertTinyReReplyBoard(map);
	}
	
	
}










