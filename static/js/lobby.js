let stompClient = null;
let currentSessionId = null;
let keepAliveInterval = null;

// WebSocket 연결 함수
function connect() {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    // Heartbeat 설정
    stompClient.heartbeat.outgoing = 10000; // 클라이언트에서 서버로 Heartbeat 간격
    stompClient.heartbeat.incoming = 10000; // 서버에서 클라이언트로 Heartbeat 간격

    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);

        // 로비 참가
        sendJoin();

        // 사용자 목록 구독
        stompClient.subscribe('/topic/lobbyUsers', function(message) {
            console.log("User list message received:", message.body);
            try {
                var userList = JSON.parse(message.body);
                console.log("Parsed user list:", userList);
                updateUserList(userList);
            } catch (e) {
                console.error("Failed to parse user list:", e);
            }
        });

        // 방 목록 구독
        stompClient.subscribe('/topic/roomList', function(message) {
            console.log("Room list message received:", message.body);
            try {
                var roomList = JSON.parse(message.body);
                console.log("Parsed room list:", roomList);
                updateRoomList(roomList);
            } catch (e) {
                console.error("Failed to parse room list:", e);
            }
        });

        // 채팅 구독
        stompClient.subscribe('/topic/chat', function(message) {
            console.log('Received chat message:', message.body);
            try {
                var chatMessage = JSON.parse(message.body);
                console.log("Parsed chat message:", chatMessage);
                showChatMessage(chatMessage);
            } catch (e) {
                console.error("Failed to parse chat message:", e);
            }
        });

        // Keep-alive 시작
        startKeepAlive();
    }, function(error) {
        console.error('Connection error: ' + error);
        setTimeout(connect, 5000); // 5초 후에 재연결 시도
    });
}

// 간단한 세션 ID 생성 함수 (UUID 방식 활용)
function generateSessionId() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// 로비에 참가
function sendJoin() {
    var nickname = window.appData.nickname || 'anonymous';
    if (!currentSessionId || !nickname || nickname.trim() === "") {
        console.error("Session ID or nickname is missing. Cannot join the lobby.");
        return;
    }
    console.log("Sending join with sessionId:", currentSessionId, "and nickname:", nickname);
    stompClient.send("/app/user.join", {}, JSON.stringify({ sessionId: currentSessionId, nickname: nickname }));
}

// 메시지 보내기
function sendMessage() {
    var messageContent = document.getElementById('chatInput').value.trim();
    var nickname = window.appData.nickname || 'anonymous';
    if (messageContent && stompClient) {
        var chatMessage = {
            sender: nickname,
            content: messageContent,
            type: 'CHAT'
        };
        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        document.getElementById('chatInput').value = '';
    }
}

// 채팅 메시지 표시
function showChatMessage(message) {
    var messagesElement = document.getElementById('chatMessages');
    var messageElement = document.createElement('p');

    messageElement.innerHTML = '<strong>' + message.sender + ':</strong> ' + message.content;
    messagesElement.appendChild(messageElement);

    // Scroll to the bottom
    messagesElement.scrollTop = messagesElement.scrollHeight;
}

// 사용자 목록 업데이트
function updateUserList(userList) {
    var userListElement = document.getElementById('userList');
    userListElement.innerHTML = ''; // 기존 목록 초기화

    if (userList && userList.length > 0) {
        userList.forEach(function(user) {
            var li = document.createElement('li');
            li.textContent = user.nickname; // 닉네임 설정
            userListElement.appendChild(li); // 목록에 추가
        });
    }
}

// 방 목록 업데이트
function updateRoomList(rooms) {
    var roomListDiv = document.getElementById("roomList");
    roomListDiv.innerHTML = '';

    rooms.forEach(function(room) {
        var roomDiv = document.createElement("div");
        roomDiv.classList.add("room");
        roomDiv.dataset.roomId = room.id;

        var roomTitle = document.createElement("h4");
        roomTitle.textContent = room.name + (room.privateRoom ? " 🔒" : "");
        roomDiv.appendChild(roomTitle);

        var roomPlayers = document.createElement("p");
        roomPlayers.textContent = "Players: " + room.currentUsers + "/" + room.maxUsers;
        roomDiv.appendChild(roomPlayers);

        roomDiv.addEventListener("click", function() {
            joinRoom(room);
        });

        roomListDiv.appendChild(roomDiv);
    });
}

// 방 참여 함수
function joinRoom(room) {
    leaveLobby()
    .then(() => {
        if (!window.appData.nickname || window.appData.nickname.trim() === "") {
            alert("방에 참여하기 위해서는 닉네임을 입력해야 합니다.");
            return;
        }

        if (room.privateRoom) {
            var password = prompt("비밀번호를 입력하세요:");
            if (password !== null) {
                attemptJoinRoom(room.id, password);
                startKeepAlive();
            }
        } else {
            attemptJoinRoom(room.id, null);
            startKeepAlive();
        }
    })
    .catch(error => {
        console.error('Error leaving lobby:', error);
        // Proceed to join room even if leaveLobby failed
        if (!window.appData.nickname || window.appData.nickname.trim() === "") {
            alert("방에 참여하기 위해서는 닉네임을 입력해야 합니다.");
            return;
        }

        if (room.privateRoom) {
            var password = prompt("비밀번호를 입력하세요:");
            if (password !== null) {
                attemptJoinRoom(room.id, password);
                startKeepAlive();
            }
        } else {
            attemptJoinRoom(room.id, null);
            startKeepAlive();
        }
    });
}

// 방 참여 요청 함수
function attemptJoinRoom(roomId, password) {
    fetch('/api/rooms/join', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            roomId: roomId,
            password: password
        })
    })
    .then(response => {
        if (response.ok) {
            window.location.href = '/rooms/' + roomId;
        } else {
            return response.text().then(text => { throw new Error(text); });
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert(error.message);
    });
}

// 룸을 떠날 때 호출되는 함수
function leaveRoom() {
    stopKeepAlive();
}

// 로비를 떠날 때 호출되는 함수
function leaveLobby() {
    return new Promise((resolve, reject) => {
        var nickname = window.appData.nickname || 'anonymous';
        if (stompClient && stompClient.connected) {
            console.log("Leaving lobby with sessionId:", currentSessionId);
            stompClient.send("/app/leaveLobby", {}, JSON.stringify({ sessionId: currentSessionId, nickname: nickname }));
            resolve(); // 성공 시
        } else {
            reject("Stomp client not connected");
        }
    });
}

// Keep-Alive 기능
function startKeepAlive() {
    if (keepAliveInterval) {
        // 이미 Keep-Alive가 시작된 경우 중복 시작 방지
        return;
    }
    console.log("Starting keep-alive");
    keepAliveInterval = setInterval(function() {
        fetch('/keepAlive', {
            method: 'GET',
            credentials: 'include'
        })
        .then(() => {
            console.log("Keep-alive ping sent");
        })
        .catch(error => console.error('Keep-alive error:', error));
    }, 60000); // 60초마다 Keep-alive 요청
}

function stopKeepAlive() {
    if (keepAliveInterval) {
        clearInterval(keepAliveInterval);
        keepAliveInterval = null;
        console.log("Stopped keep-alive");
    }
}

// 로그아웃 기능
function logout() {
    leaveLobby()
    .then(() => {
        const csrfToken = getCsrfToken();
        const csrfHeader = getCsrfHeader();

        return fetch('/logout', {
            method: 'POST', // POST 메서드 사용
            credentials: 'include', // 쿠키 포함
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken // CSRF 토큰 헤더 포함
            }
        });
    })
    .then(response => {
        if (response.ok) {
            // 로그아웃 성공 시 세션 무효화가 서버에서 처리됨
            window.location.href = '/'; // 로그아웃 후 홈으로 이동
        } else {
            return response.text().then(text => { throw new Error(text); });
        }
    })
    .catch(error => {
        console.error('Logout Error:', error);
        alert('로그아웃에 실패했습니다.');
    });
}

// CSRF 토큰 가져오기 함수
function getCsrfToken() {
    const token = document.querySelector('meta[name="_csrf"]');
    return token ? token.getAttribute('content') : '';
}

function getCsrfHeader() {
    const header = document.querySelector('meta[name="_csrf_header"]');
    return header ? header.getAttribute('content') : '';
}

// 페이지 로드 시 초기화
window.onload = function() {
    ensureUserInfoElementsExist();
    console.log("Page loaded");

    // 세션 ID 초기화
    if (sessionStorage.getItem('sessionId')) {
        currentSessionId = sessionStorage.getItem('sessionId');
        console.log("Retrieved sessionId from sessionStorage:", currentSessionId);
    } else {
        currentSessionId = generateSessionId();
        sessionStorage.setItem('sessionId', currentSessionId);
        console.log("Generated new sessionId and stored in sessionStorage:", currentSessionId);
    }

    var nickname = window.appData.nickname || '';
    console.log("Initial nickname:", nickname);
    if (nickname.trim() !== "") {
        // sessionStorage에 nickname 저장 (옵션)
        sessionStorage.setItem('nickname', nickname);
        console.log("Using nickname from injected variable:", nickname);
        fetchUserInfo();
        connect();
        fetchRoomList();
    } else {
        console.log("No nickname from injected variable, prompting user...");
        nickname = prompt("닉네임을 입력하세요:");
        if (nickname && nickname.trim() !== "") {
            window.appData.nickname = nickname; // 전역 변수 업데이트
            sessionStorage.setItem('nickname', nickname);
            console.log("User entered nickname:", nickname);
            fetchUserInfo();
            connect();
            fetchRoomList();
        } else {
            alert("닉네임을 입력하지 않으면 로비에 참여할 수 없습니다.");
        }
    }

    // Logout 버튼 이벤트 리스너 설정
    var logoutBtn = document.getElementById('logoutButton');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', logout);
    } else {
        console.error("Logout 버튼을 찾을 수 없습니다.");
    }
};

// 페이지를 벗어나거나 브라우저를 닫을 때 처리
window.addEventListener('beforeunload', function(event) {
    leaveLobby()
    .catch(error => {
        console.error('Failed to leave lobby:', error);
    });

    stopKeepAlive();
});

// 방 생성 버튼 클릭 시 모달 열기
var createRoomBtn = document.getElementById("createRoomButton");
var modal = document.getElementById("createRoomModal");
var span = modal.querySelector(".close");

if (createRoomBtn) {
    createRoomBtn.onclick = function() {
        if (!window.appData.nickname || window.appData.nickname.trim() === "") {
            alert("방을 생성하기 위해서는 닉네임을 입력해야 합니다.");
        } else {
            modal.style.display = "block";
        }
    }
}

// 방 찾기 버튼 클릭 시 동작 추가 (필요 시 구현)
var findRoomBtn = document.getElementById("findRoomButton");
if (findRoomBtn) {
    findRoomBtn.onclick = function() {
        alert("방 찾기 기능은 아직 구현되지 않았습니다.");
    }
}

// 새로고침 버튼 클릭 시 방 목록 다시 가져오기
var refreshBtn = document.getElementById("refreshButton");
if (refreshBtn) {
    refreshBtn.onclick = function() {
        fetchRoomList();
    }
}

// 모달의 X 버튼 클릭 시 모달 닫기
if (span) {
    span.onclick = function() {
        modal.style.display = "none";
    }
}

// 모달 바깥 영역 클릭 시 모달 닫기
window.onclick = function(event) {
    if (event.target == modal) {
        modal.style.display = "none";
    }
}

// 방 생성 폼 제출 시 이벤트 처리
document.getElementById("createRoomForm").addEventListener("submit", function(event) {
    event.preventDefault();

    var roomName = document.getElementById("roomName").value.trim();
    var maxPlayers = parseInt(document.getElementById("maxPlayers").value);
    var password = document.getElementById("password").value.trim();

    // 서버로 방 생성 요청 보내기
    createRoom(roomName, maxPlayers, password);

    // 모달 닫기
    modal.style.display = "none";
});

// 방 생성 함수
function createRoom(roomName, maxUsers, password) {
    fetch('/api/rooms/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            name: roomName,
            maxUsers: maxUsers,
            password: password,
            privateRoom: password && password.trim() !== "" // privateRoom 필드 추가
        })
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text); });
        }
        return response.json();
    })
    .then(data => {
        if (data.id) {
            window.location.href = '/rooms/' + data.id;
        } else {
            throw new Error("방 ID가 응답에 포함되지 않았습니다.");
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert("방 생성 실패: " + error.message);
    });
}

// 방 목록 가져오기 함수
function fetchRoomList() {
    fetch('/api/rooms/list')
    .then(response => response.json())
    .then(rooms => {
        updateRoomList(rooms);
    })
    .catch(error => console.error('Error:', error));
}

// 사용자가 로그인 되어있는지 확인하는 함수 (예시)
function isUserLoggedIn() {
    return sessionStorage.getItem('isLoggedIn') === 'true'; // 로그인 여부를 세션 저장소에 저장
}

document.getElementById('sendButton').addEventListener('click', function() {
    sendMessage();
});

// Enter 키 입력 시 메시지 보내기
document.getElementById('chatInput').addEventListener('keydown', function(event) {
    if (event.key === 'Enter') {
        event.preventDefault();  // 기본 Enter 동작 방지
        sendMessage();
    }
});

function fetchUserInfo() {
    fetch('/api/user/info') // 사용자 정보를 불러오는 API
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text); });
            }
            return response.json();
        })
        .then(userInfo => {
            // DOM 요소의 ID를 사용해 업데이트
            const nicknameElement = document.getElementById('userNickname');
            if (nicknameElement) {
                nicknameElement.textContent = userInfo.nickname || 'anonymous';
            } else {
                console.error('Nickname element not found in DOM');
            }

            const winsElement = document.getElementById('userWins');
            if (winsElement) {
                winsElement.textContent = userInfo.wins || 0;
            }

            const drawsElement = document.getElementById('userDraws');
            if (drawsElement) {
                drawsElement.textContent = userInfo.draws || 0;
            }

            const lossesElement = document.getElementById('userLosses');
            if (lossesElement) {
                lossesElement.textContent = userInfo.losses || 0;
            }

            const winRateElement = document.getElementById('userWinRate');
            if (winRateElement) {
                winRateElement.textContent = userInfo.winRate ? userInfo.winRate.toFixed(2) : '0';
            }
        })
        .catch(error => {
            console.error('Failed to fetch user info:', error);
            alert('사용자 정보를 가져오는 중 오류가 발생했습니다: ' + error.message);
        });
}

function ensureUserInfoElementsExist() {
    let userInfoContainer = document.querySelector('.user-info');
    if (!userInfoContainer) {
        userInfoContainer = document.createElement('div');
        userInfoContainer.className = 'user-info';
        document.body.appendChild(userInfoContainer); // 원하는 위치에 추가
    }

    ['nickname', 'wins', 'draws', 'losses', 'winRate'].forEach(key => {
        let element = userInfoContainer.querySelector(`span[th\\:text="\${${key}}"]`);
        if (!element) {
            element = document.createElement('span');
            element.setAttribute('th:text', `\${${key}}`);
            userInfoContainer.appendChild(element);
        }
    });
}
