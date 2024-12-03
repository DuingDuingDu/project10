package com.hoit.checkers.service;

import com.hoit.checkers.model.User;
import com.hoit.checkers.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class UserService implements UserDetailsService {
	
	private final PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // 사용자 세션 관리를 위한 맵 (사용자 이름 -> 세션)
    private final ConcurrentMap<String, HttpSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
   
    /**
     * 사용자 인증 메서드
     * @param username 사용자 이름
     * @param password 비밀번호
     * @return 인증된 사용자 객체 또는 null
     */
    public User authenticate(String username, String password) {
        if (isNullOrEmpty(username) || isNullOrEmpty(password)) {
            logger.debug("인증 실패: 유효하지 않은 입력값 - username 또는 password 비어있음");
            return null;
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            logger.debug("사용자 찾음 - username: {}", user.getUsername());
            if (passwordEncoder.matches(password, user.getPassword())) {
                logger.debug("비밀번호 일치 - username: {}", username);
                
                // userType이 null이면 MEMBER로 설정
                if (user.getUserType() == null) {
                    user.setUserType(User.UserType.MEMBER);
                    userRepository.save(user); // 변경사항 저장
                }
                
                return user;
            } else {
                logger.debug("비밀번호 불일치 - username: {}", username);
            }
        } else {
            logger.debug("사용자를 찾을 수 없음 - username: {}", username);
        }
        return null;
    }

    /**
     * 닉네임으로 사용자 찾기
     * @param nickname 사용자 닉네임
     * @return 사용자 객체
     * @throws UsernameNotFoundException 사용자 찾을 수 없음
     */
    public User findByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with nickname: " + nickname));
    }

    /**
     * 사용자명으로 사용자 찾기
     * @param username 사용자 이름
     * @return 사용자 객체
     * @throws UsernameNotFoundException 사용자 찾을 수 없음
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    /**
     * 사용자명과 닉네임으로 사용자 찾기
     * @param username 사용자 이름
     * @param nickname 사용자 닉네임
     * @return Optional 사용자 객체
     */
    public Optional<User> findByUsernameAndNickname(String username, String nickname) {
        return userRepository.findByUsernameAndNickname(username, nickname);
    }

    /**
     * 사용자 저장 메서드
     * @param user 저장할 사용자 객체
     * @return 저장된 사용자 객체
     * @throws IllegalArgumentException 게스트 유저 또는 userType이 설정되지 않은 경우
     */
    public User save(User user) {
        validateUserForSave(user);
        return userRepository.save(user);
    }

    /**
     * 사용자 세션 등록
     * @param username 사용자 이름
     * @param session HTTP 세션
     */
    public synchronized void registerUserSession(String username, HttpSession session) {
        // 기존 세션이 존재하면 무효화
        HttpSession oldSession = userSessions.put(username, session);
        if (oldSession != null && !oldSession.equals(session)) {
            try {
                oldSession.invalidate();
                logger.debug("기존 세션 무효화 - username: {}", username);
            } catch (IllegalStateException e) {
                logger.warn("기존 세션 무효화 중 예외 발생 - 이미 무효화된 세션일 수 있음: {}", username);
            }
        }
    }

    /**
     * 사용자 세션 제거
     * @param username 사용자 이름
     */
    public synchronized void removeUserSession(String username) {
        userSessions.remove(username);
    }

    /**
     * 사용자 세션 가져오기
     * @param username 사용자 이름
     * @return HTTP 세션 객체 또는 null
     */
    public HttpSession getUserSession(String username) {
        return userSessions.get(username);
    }

    /**
     * 현재 사용자 가져오기
     * @param session HTTP 세션
     * @return 사용자 객체
     */
    public User getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            user = (User) session.getAttribute("guestUser"); // 게스트 사용자 확인
        }

        if (user == null) {
            logger.warn("세션에서 사용자를 찾을 수 없습니다. 세션 ID: {}", session.getId());
        }

        return user;
    }

    /**
     * 게스트 사용자 가져오기 또는 새로 생성
     * @param session HTTP 세션
     * @return 게스트 사용자 객체
     */
    public User getGuestFromSessionOrCreate(HttpSession session) {
        User guestUser = (User) session.getAttribute("guestUser");

        if (guestUser == null) {
            // 새로운 게스트 사용자 생성
            String nickname = "guest_" + UUID.randomUUID().toString().substring(0, 8);
            guestUser = new User();
            guestUser.setUsername(nickname);
            guestUser.setNickname(nickname);
            guestUser.setUserType(User.UserType.GUEST);
            
            // 세션에 게스트 정보 저장
            session.setAttribute("guestUser", guestUser);
            logger.debug("새 게스트 사용자 생성 - nickname: {}", nickname);
        }

        return guestUser;
    }

    /**
     * 게스트 사용자 생성 메서드
     * @param session HTTP 세션
     * @param nickname 게스트 닉네임
     * @return 생성된 게스트 사용자 객체
     */
    public User createGuestUser(HttpSession session, String nickname) {
        if (isNullOrEmpty(nickname)) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }

        // 게스트 사용자 생성 (DB에 저장하지 않음)
        User guestUser = new User();
        guestUser.setUsername("guest_" + UUID.randomUUID().toString().substring(0, 8));
        guestUser.setNickname(nickname);
        guestUser.setUserType(User.UserType.GUEST);

        // 세션에 게스트 정보 저장
        session.setAttribute("guestNickname", nickname);
        session.setAttribute("user", guestUser);

        logger.debug("새 게스트 사용자 생성 - nickname: {}", nickname);
        return guestUser;
    }
    
    /**
     * 게스트 사용자 생성 또는 기존 게스트 사용자 반환 메서드
     * @param session HTTP 세션
     * @param nickname 게스트 닉네임
     * @return 생성되거나 기존의 게스트 사용자 객체
     * @throws Exception 닉네임 중복 시 예외
     */
    @Transactional
    public User createOrGetGuestUser(HttpSession session, String nickname) throws Exception {
        if (isNullOrEmpty(nickname)) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }

        // 닉네임으로 사용자 찾기
        Optional<User> optionalUser = userRepository.findByNickname(nickname);
        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();
            if (existingUser.getUserType() == User.UserType.GUEST) {
                // 세션에 사용자 정보 저장
                session.setAttribute("guestNickname", nickname);
                session.setAttribute("user", existingUser);
                logger.debug("기존 게스트 사용자 반환 - nickname: {}", nickname);
                return existingUser;
            } else {
                throw new Exception("닉네임이 이미 사용 중입니다.");
            }
        }

        // 새로운 게스트 사용자 생성
        User guestUser = new User();
        guestUser.setUsername("guest_" + UUID.randomUUID().toString().substring(0, 8));
        guestUser.setNickname(nickname);
        guestUser.setPassword(passwordEncoder.encode("guest_password")); // 실제 운영 시 적절한 패스워드 설정 또는 암호화
        guestUser.setUserType(User.UserType.GUEST);
        guestUser.setWins(0);
        guestUser.setDraws(0);
        guestUser.setLosses(0);
        guestUser.setWinRate(0.0);

        // 게스트 사용자 저장
        userRepository.save(guestUser);
        logger.debug("새 게스트 사용자 생성 및 저장 - nickname: {}", nickname);

        // 세션에 게스트 정보 저장
        session.setAttribute("guestNickname", nickname);
        session.setAttribute("user", guestUser);

        return guestUser;
    }

    /**
     * 사용자 저장 전 검증 메서드
     * @param user 사용자 객체
     */
    private void validateUserForSave(User user) {
        if (user.getUserType() == null) {
            throw new IllegalArgumentException("userType이 설정되지 않았습니다.");
        }
        if (user.getUserType() == User.UserType.GUEST) {
            throw new IllegalArgumentException("게스트 유저는 저장할 수 없습니다.");
        }
    }

    /**
     * 문자열이 null이거나 빈 문자열인지 확인
     * @param str 확인할 문자열
     * @return true if null or empty, false otherwise
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Spring Security의 UserDetailsService 메서드 구현
     * @param username 사용자 이름
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자 찾을 수 없음
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // 이미 bcrypt로 인코딩된 비밀번호
                .roles(user.getUserType().toString()) // 사용자의 역할을 설정
                .build();
    }
}
