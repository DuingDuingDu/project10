// 전역 변수
let roomId = '';
let user = {};
let userRole = '';
let isHost = false;
let stompClient = null;
let isReady = false;
let gameStarted = false;

// 메인 초기화 함수
function initialize() {
    initializeUI();    // UI 먼저 초기화
    connect();         // WebSocket 연결
     // 게임 보드 초기화

    // 이벤트 리스너 설정
    const sendButton = document.getElementById('send-button');
    const chatInput = document.getElementById('chat-input');
    const leaveButton = document.getElementById('leave-room-button');
    const readyButton = document.getElementById('ready-button');
    const startButton = document.getElementById('start-button');

    if (sendButton) {
        sendButton.addEventListener('click', sendMessage);
    }
    if (chatInput) {
        chatInput.addEventListener('keyup', function (event) {
            if (event.key === 'Enter') {
                sendMessage();
            }
        });
    }
    if (leaveButton) {
        leaveButton.addEventListener('click', leaveRoom);
    }
    if (readyButton) {
        readyButton.addEventListener('click', toggleReady);
    }
    if (startButton) {
        startButton.addEventListener('click', sendStartGame);
    }
}

// UI 초기화
function initializeUI() {
    if (userRole === 'OBSERVER') {
        // Observer UI 설정
        document.getElementById('button-area').style.display = 'none';
        document.getElementById('observer-list').style.display = 'block';
        document.getElementById('left-player-title').textContent = 'Player 1';
        document.getElementById('right-player-title').textContent = 'Player 2';
        return;
    }

    // Player1 또는 Player2 모두 동일하게 처리
    // 자신의 정보는 항상 왼쪽에
    document.getElementById('left-player-title').textContent = '내 정보';
    document.getElementById('leftPlayerNickname').textContent = user.nickname;
    document.getElementById('leftPlayerWins').textContent = user.wins;
    document.getElementById('leftPlayerDraws').textContent = user.draws;
    document.getElementById('leftPlayerLosses').textContent = user.losses;
    document.getElementById('leftPlayerWinRate').textContent = user.winRate;

    // 버튼 표시 (Ready는 모든 플레이어에게, Start는 호스트에게만)
    document.getElementById('ready-button').style.display = 'block';
    document.getElementById('start-button').style.display = isHost ? 'block' : 'none';

    // 상대방 영역 제목 설정
    document.getElementById('right-player-title').textContent = '상대방 정보';
}

// WebSocket 연결 설정
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
	
	stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

		
		setupSubscriptions();
        

        sendJoinRoom();
        onJoinRoom(userRole, roomId, user.nickname);
		setTimeout(() => {
		           initializeGame();
		       }, 100);
    }, function (error) {
        console.error('Connection error: ' + error);
    });
}

function setupSubscriptions() {
    // 채팅 구독
    stompClient.subscribe('/topic/chat/' + roomId, handleChatMessage);
    // 게임 관련 구독
    stompClient.subscribe('/topic/room/' + roomId + '/ready', handleReadyStatus);
    stompClient.subscribe('/topic/game/' + roomId + '/start', handleGameStart);
    stompClient.subscribe(`/topic/room/${roomId}/opponent`, handleOpponentUpdate);
    stompClient.subscribe(`/topic/room/${roomId}/playerPromotion`, handlePlayerPromotion);
    stompClient.subscribe(`/topic/room/${roomId}/player2Left`, handlePlayer2Left);
    stompClient.subscribe(`/topic/playerRoleChanged`, handleRoleChange);
}

// WebSocket 메시지 핸들러
function handleChatMessage(message) {
    const chatMessage = JSON.parse(message.body);
    showChatMessage(chatMessage);
}

function handleReadyStatus(message) {
    const readyStatus = JSON.parse(message.body);
    updateReadyStatus(readyStatus);
}

function handleGameStart(message) {
    const gameStartResponse = JSON.parse(message.body);
    startGame(gameStartResponse);
}

function handleOpponentUpdate(message) {
    try {
        console.log('Received opponent update:', message.body);
        const opponentInfo = JSON.parse(message.body);
        
        // Observer가 아닐 때만 상대방 정보 업데이트
        if (userRole !== 'OBSERVER') {
            // userRole이 PLAYER2일 때는 PLAYER1의 정보를, PLAYER1일 때는 PLAYER2의 정보를 표시
            if ((userRole === 'PLAYER2' && opponentInfo.role === 'PLAYER1') ||
                (userRole === 'PLAYER1' && opponentInfo.role === 'PLAYER2')) {
                document.getElementById('rightPlayerNickname').textContent = opponentInfo.nickname;
                document.getElementById('rightPlayerWins').textContent = opponentInfo.wins;
                document.getElementById('rightPlayerDraws').textContent = opponentInfo.draws;
                document.getElementById('rightPlayerLosses').textContent = opponentInfo.losses;
                document.getElementById('rightPlayerWinRate').textContent = opponentInfo.winRate + '%';
            }
        } else {
            // Observer의 경우 PLAYER1은 왼쪽, PLAYER2는 오른쪽에 표시
            if (opponentInfo.role === 'PLAYER1') {
                document.getElementById('leftPlayerNickname').textContent = opponentInfo.nickname;
                document.getElementById('leftPlayerWins').textContent = opponentInfo.wins;
                document.getElementById('leftPlayerDraws').textContent = opponentInfo.draws;
                document.getElementById('leftPlayerLosses').textContent = opponentInfo.losses;
                document.getElementById('leftPlayerWinRate').textContent = opponentInfo.winRate + '%';
            } else if (opponentInfo.role === 'PLAYER2') {
                document.getElementById('rightPlayerNickname').textContent = opponentInfo.nickname;
                document.getElementById('rightPlayerWins').textContent = opponentInfo.wins;
                document.getElementById('rightPlayerDraws').textContent = opponentInfo.draws;
                document.getElementById('rightPlayerLosses').textContent = opponentInfo.losses;
                document.getElementById('rightPlayerWinRate').textContent = opponentInfo.winRate + '%';
            }
        }
    } catch (error) {
        console.error('Error parsing opponent update:', error);
    }
}

function handlePlayerPromotion(message) {
    const promotionInfo = JSON.parse(message.body);
    if (promotionInfo.nickname === user.nickname) {
        userRole = 'PLAYER2';
        document.getElementById('promotion-area').style.display = 'none';
        document.getElementById('ready-button').style.display = 'block';
        
        const notification = {
            sender: 'System',
            content: `${user.nickname}님이 Player2가 되었습니다.`,
            type: 'CHAT',
            roomId: roomId
        };
        stompClient.send("/app/chat.send", {}, JSON.stringify(notification));
    }
}

function handlePlayer2Left(message) {
    if (userRole === 'OBSERVER') {
        document.getElementById('promotion-area').style.display = 'block';
        document.getElementById('rightPlayerNickname').textContent = '';
        document.getElementById('rightPlayerWins').textContent = '0';
        document.getElementById('rightPlayerDraws').textContent = '0';
        document.getElementById('rightPlayerLosses').textContent = '0';
        document.getElementById('rightPlayerWinRate').textContent = '0%';
    }
}

function handleRoleChange(message) {
    const roleUpdate = JSON.parse(message.body);
    if (roleUpdate.newRole === 'PLAYER2' && userRole === 'OBSERVER') {
        document.getElementById('promotion-area').style.display = 'none';
    }
}

// 게임 관련 함수
function sendStartGame() {
    if (stompClient) {
        const gameStartMessage = {
            roomId: roomId,
            username: user.username
        };
        stompClient.send("/app/game.start", {}, JSON.stringify(gameStartMessage));
    }
}

function startGame(gameStartResponse) {
    gameStarted = true;
    console.log(gameStartResponse.message);

    document.getElementById('leave-room-button').style.display = 'none';
    document.getElementById('ready-button').style.display = 'none';
    document.getElementById('start-button').style.display = 'none';

    showInGameButtons();
    initializeGame();
}

function showInGameButtons() {
    const buttonArea = document.getElementById('button-area');
    buttonArea.innerHTML = '';

    const surrenderButton = document.createElement('button');
    surrenderButton.id = 'surrender-button';
    surrenderButton.textContent = '기권';
    surrenderButton.classList.add('in-game-button');
    surrenderButton.addEventListener('click', surrenderGame);
    buttonArea.appendChild(surrenderButton);

    const drawButton = document.createElement('button');
    drawButton.id = 'draw-button';
    drawButton.textContent = '무승부 제안';
    drawButton.classList.add('in-game-button');
    drawButton.addEventListener('click', offerDraw);
    buttonArea.appendChild(drawButton);
}

// 방 관련 함수들
function sendJoinRoom() {
    const joinMessage = {
        roomId: roomId,
        nickname: user.nickname
    };
    console.log('Sending join notification:', joinMessage);
    stompClient.send("/app/room.join", {}, JSON.stringify(joinMessage));
}

function onJoinRoom(userRole, roomId, username) {
    // Observer가 아닌 경우, 항상 자신은 왼쪽에 표시
    if (userRole !== 'OBSERVER') {
        document.getElementById('leftPlayerNickname').textContent = username;
    }
}

function requestPromotionToPlayer() {
    if (stompClient) {
        const promotionRequest = {
            roomId: roomId,
            nickname: user.nickname
        };
        stompClient.send("/app/room.requestPromotion", {}, JSON.stringify(promotionRequest));
    }
}

// 채팅 관련 함수들
function sendMessage() {
    const chatInput = document.getElementById('chat-input');
    const messageContent = chatInput.value.trim();
    
    if (messageContent && stompClient) {
        const chatMessage = {
            sender: user.nickname,
            content: messageContent,
            type: 'CHAT',
            roomId: roomId
        };
        
        try {
            stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
            chatInput.value = '';  // 메시지 전송 후 입력창 비우기
        } catch (error) {
            console.error('Error sending message:', error);
        }
    }
}

function showChatMessage(message) {
    const messagesElement = document.getElementById('messages');
    const messageElement = document.createElement('p');
    messageElement.innerHTML = `<strong>${message.sender}:</strong> ${message.content}`;
    messagesElement.appendChild(messageElement);
    messagesElement.scrollTop = messagesElement.scrollHeight;
}

// Ready 상태 관련 함수들
function toggleReady() {
    isReady = !isReady;
    document.getElementById('ready-button').textContent = isReady ? 'Cancel Ready' : 'Ready';
    sendReadyStatus(isReady);

    const messageContent = isReady ? `${user.nickname}님이 준비하셨습니다.` : `${user.nickname}님이 준비를 취소하셨습니다.`;
    const chatMessage = {
        sender: 'System',
        content: messageContent,
        type: 'CHAT',
        roomId: roomId
    };
    stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
}

function sendReadyStatus(ready) {
    if (stompClient) {
        const readyMessage = {
            roomId: roomId,
            username: user.username,
            ready: ready
        };
        stompClient.send('/app/room.ready', {}, JSON.stringify(readyMessage));
    }
}

function updateReadyStatus(readyStatus) {
    console.log('Ready status update received:', readyStatus);
    console.log('Current user role:', userRole);
    console.log('allReady status:', readyStatus.allReady);

    if (userRole === 'PLAYER1' && readyStatus.allReady) {
        console.log('Showing start button for PLAYER1');
        document.getElementById('start-button').style.display = 'block';
    } else {
        console.log('Hiding start button');
        document.getElementById('start-button').style.display = 'none';
    }
}
// 게임 보드 초기화
function initializeGame() {
    const stage = new Konva.Stage({
        container: 'konva-stage',
        width: 900,
        height: 900
    });

    const layer = new Konva.Layer();
    stage.add(layer);

    // Player2일 때만 보드 회전
    if (userRole === 'PLAYER2') {
        stage.rotation(180);
        stage.x(stage.width());
        stage.y(stage.height());
    }

    drawCheckerBoard(layer);
    setupPieces(layer);
    layer.draw();
}

function drawCheckerBoard(layer) {
    const squareSize = 90;
    for (let row = 0; row < 10; row++) {
        for (let col = 0; col < 10; col++) {
            const rect = new Konva.Rect({
                x: col * squareSize,
                y: row * squareSize,
                width: squareSize,
                height: squareSize,
                fill: (row + col) % 2 === 1 ? '#8B4513' : '#F8F0E3', // 조건 변경
                id: `square-${row}-${col}`
            });
            layer.add(rect);
        }
    }
}

function setupPieces(layer) {
    const squareSize = 90;
    const pieceRadius = 30;
    const blackPieces = '#3A271B';
    const whitePieces = '#F8F0E3';

    function createPiece(x, y, isPlayer1Piece, isMyPiece) {
        const color = isPlayer1Piece ? blackPieces : whitePieces;
        const piece = new Konva.Circle({
            x: x * squareSize + squareSize / 2,
            y: y * squareSize + squareSize / 2,
            radius: pieceRadius,
            fill: color,
            stroke: '#000000',
            strokeWidth: 2,
            draggable: isMyPiece && gameStarted,
            id: `piece-${x}-${y}`
        });

        piece.on('dragstart', () => handleDragStart(piece));
        piece.on('dragend', () => handleDragEnd(piece));

        return piece;
    }

    // 위쪽 기물 배치 (row 0-3)
    for (let row = 0; row < 4; row++) {
        const startCol = row % 2 === 0 ? 1 : 0;  // 짝수 행은 1부터, 홀수 행은 0부터
        for (let col = startCol; col < 10; col += 2) {
            const isOpponentPiece = userRole === 'PLAYER1';  // PLAYER1은 상대가 흰색
            const piece = createPiece(col, row, !isOpponentPiece, false);
            if (piece) {
                layer.add(piece);
            }
        }
    }

    // 아래쪽 기물 배치 (row 6-9)
    for (let row = 6; row < 10; row++) {
        const startCol = row % 2 === 0 ? 1 : 0;  // 짝수 행은 1부터, 홀수 행은 0부터
        for (let col = startCol; col < 10; col += 2) {
            const isMyPiece = userRole === 'PLAYER1';  // PLAYER1은 검은색
            const piece = createPiece(col, row, isMyPiece, true);
            if (piece) {
                layer.add(piece);
            }
        }
    }

    layer.draw();

    // 게임 상태 저장
    if (stompClient && stompClient.connected) {
        const gameState = {
            roomId: roomId,
            currentTurn: userRole,
            pieces: collectPiecesState()
        };
        setTimeout(() => {
            stompClient.send("/app/game.state", {}, JSON.stringify(gameState));
        }, 500);
    }
}

function saveGameState() {
    if (stompClient && stompClient.connected) {
        const gameState = {
            roomId: roomId,
            currentTurn: userRole,
            pieces: collectPiecesState()
        };
        stompClient.send("/app/game.state", {}, JSON.stringify(gameState));
    }
}

// 게임 상태 로드
function loadGameState() {
    if (stompClient) {
        stompClient.send("/app/game.getState", {}, JSON.stringify({
            roomId: roomId
        }));
    }
}


// 방 나가기 관련 함수
function leaveRoom() {
    const csrfMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    if (!csrfMeta || !csrfHeaderMeta) {
        console.error('CSRF 메타 태그가 존재하지 않습니다.');
        alert('보안 토큰을 찾을 수 없어 요청을 처리할 수 없습니다.');
        return;
    }

    const csrfToken = csrfMeta.getAttribute('content');
    const csrfHeader = csrfHeaderMeta.getAttribute('content');

    fetch('/api/rooms/leave', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify({
            roomId: roomId,
            username: user.username
        })
    })
    .then(response => {
        if (response.ok) {
            window.location.href = '/lobby';
        } else {
            return response.text().then(text => { throw new Error(text); });
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert(error.message);
    });
}

// DOMContentLoaded 이벤트 리스너
document.addEventListener('DOMContentLoaded', function () {
    roomId = window.appData.roomId;
    user = {
        username: window.appData.user.username,
        nickname: window.appData.user.nickname,
        wins: parseInt(window.appData.user.wins),
        losses: parseInt(window.appData.user.losses),
        draws: parseInt(window.appData.user.draws),
        winRate: parseFloat(window.appData.user.winRate)
    };
    userRole = window.appData.userRole;
    isHost = window.appData.isHost === 'true';
	
	console.log('DOM Content Loaded');
	console.log('User Role:', userRole);
	    console.log('Is Host:', isHost);
		
		if (window.appData.roomState) {
		        const players = window.appData.roomState.players;
		        for (const [nickname, playerInfo] of Object.entries(players)) {
		            if (nickname !== user.nickname) {  // 자신이 아닌 플레이어 정보만 처리
		                handleOpponentUpdate({
		                    body: JSON.stringify({
		                        nickname: playerInfo.nickname,
		                        wins: playerInfo.wins,
		                        draws: playerInfo.draws,
		                        losses: playerInfo.losses,
		                        winRate: playerInfo.winRate,
		                        role: playerInfo.role
		                    })
		                });
		            }
		        }
		    }
    initialize();
});


function collectPiecesState() {
    // 현재 게임판의 상태를 수집하는 로직
    const pieces = [];
    const stage = Konva.stages[0];
    const layer = stage.findOne('Layer');
    
    layer.find('Circle').forEach(piece => {
        pieces.push({
            x: Math.floor(piece.x() / 90),
            y: Math.floor(piece.y() / 90),
            color: piece.fill(),
            id: piece.id()
        });
    });
    
    return pieces;
}

// 창을 닫을 때 이벤트 리스너
window.addEventListener('beforeunload', function (event) {
    if (stompClient && stompClient.connected) {
        const leaveMessage = {
            roomId: roomId,
            username: user.username
        };
        stompClient.send("/app/room.leave", {}, JSON.stringify(leaveMessage));
    }
});