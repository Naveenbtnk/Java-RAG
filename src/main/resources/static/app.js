const form = document.querySelector('#chat-form');
const input = document.querySelector('#message-input');
const sendButton = document.querySelector('#send-button');
const messages = document.querySelector('#messages');
const status = document.querySelector('#system-status');
const statusLabel = document.querySelector('#status-label');
const suggestions = document.querySelector('#suggestions');

const welcomeMarkup = messages.innerHTML;

function appendMessage(role, text, typing = false) {
    const article = document.createElement('article');
    article.className = `message ${role}-message${typing ? ' typing' : ''}`;

    const avatar = document.createElement('div');
    avatar.className = 'avatar';
    avatar.setAttribute('aria-hidden', 'true');
    avatar.textContent = role === 'user' ? 'Y' : 'P';

    const bubble = document.createElement('div');
    bubble.className = 'bubble';
    const speaker = document.createElement('span');
    speaker.className = 'speaker';
    speaker.textContent = role === 'user' ? 'You' : 'Policy Lens';
    const content = document.createElement('p');
    content.textContent = text;

    bubble.append(speaker, content);
    article.append(avatar, bubble);
    messages.append(article);
    messages.scrollTop = messages.scrollHeight;
    return article;
}

async function sendMessage(message) {
    appendMessage('user', message);
    suggestions.hidden = true;
    input.value = '';
    input.style.height = 'auto';
    input.disabled = true;
    sendButton.disabled = true;
    const pending = appendMessage('assistant', 'Thinking', true);

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({message})
        });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(body.message || `Request failed (${response.status})`);
        }
        pending.remove();
        appendMessage('assistant', body.answer);
    } catch (error) {
        pending.remove();
        appendMessage('assistant', `I couldn't complete that request. ${error.message}`);
    } finally {
        input.disabled = false;
        sendButton.disabled = false;
        input.focus();
    }
}

form.addEventListener('submit', event => {
    event.preventDefault();
    const message = input.value.trim();
    if (message) sendMessage(message);
});

input.addEventListener('input', () => {
    input.style.height = 'auto';
    input.style.height = `${Math.min(input.scrollHeight, 130)}px`;
});

input.addEventListener('keydown', event => {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        form.requestSubmit();
    }
});

suggestions.addEventListener('click', event => {
    if (event.target.matches('button')) sendMessage(event.target.textContent);
});

document.querySelector('#clear-button').addEventListener('click', () => {
    messages.innerHTML = welcomeMarkup;
    suggestions.hidden = false;
    input.focus();
});

async function refreshStatus() {
    try {
        const response = await fetch('/api/ingestion/status');
        if (!response.ok) throw new Error('Status unavailable');
        const body = await response.json();
        status.classList.toggle('ready', body.state === 'COMPLETED');
        status.classList.toggle('error', body.state === 'FAILED');
        statusLabel.textContent = body.state === 'COMPLETED' ? 'Knowledge ready' : body.state.replaceAll('_', ' ').toLowerCase();
        if (body.state === 'RUNNING') setTimeout(refreshStatus, 1500);
    } catch {
        status.classList.add('error');
        statusLabel.textContent = 'Backend offline';
    }
}

refreshStatus();
