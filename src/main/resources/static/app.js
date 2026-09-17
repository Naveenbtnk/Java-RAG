const form = document.querySelector('#chat-form');
const input = document.querySelector('#message-input');
const sendButton = document.querySelector('#send-button');
const messages = document.querySelector('#messages');
const status = document.querySelector('#system-status');
const statusLabel = document.querySelector('#status-label');
const suggestions = document.querySelector('#suggestions');
const uploadForm = document.querySelector('#upload-form');
const policyFile = document.querySelector('#policy-file');
const uploadButton = document.querySelector('#upload-button');
const uploadStatus = document.querySelector('#upload-status');

const welcomeMarkup = messages.innerHTML;

function escapeHtml(value) {
    return value
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function inlineMarkdown(value) {
    return escapeHtml(value)
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/`(.+?)`/g, '<code>$1</code>');
}

function renderMarkdown(value) {
    const lines = value.replaceAll('\r\n', '\n').split('\n');
    const output = [];
    let listOpen = false;

    const closeList = () => {
        if (listOpen) {
            output.push('</ul>');
            listOpen = false;
        }
    };

    for (const line of lines) {
        const heading = line.match(/^#{1,4}\s+(.+)$/);
        const item = line.match(/^\s*[-*]\s+(.+)$/);

        if (heading) {
            closeList();
            output.push(`<div class="md-heading">${inlineMarkdown(heading[1])}</div>`);
        } else if (item) {
            if (!listOpen) {
                output.push('<ul>');
                listOpen = true;
            }
            output.push(`<li>${inlineMarkdown(item[1])}</li>`);
        } else if (!line.trim()) {
            closeList();
        } else {
            closeList();
            output.push(`<p>${inlineMarkdown(line)}</p>`);
        }
    }

    closeList();
    return output.join('');
}

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
    const content = document.createElement('div');
    content.className = 'message-content';
    if (role === 'assistant' && !typing) {
        content.innerHTML = renderMarkdown(text);
    } else {
        content.textContent = text;
    }

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

uploadForm.addEventListener('submit', async event => {
    event.preventDefault();
    const file = policyFile.files[0];
    if (!file) return;
    uploadButton.disabled = true;
    uploadStatus.className = '';
    uploadStatus.textContent = 'Uploading and indexing…';
    const formData = new FormData();
    formData.append('file', file);
    try {
        const response = await fetch('/api/documents', {method: 'POST', body: formData});
        const body = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(body.message || `Upload failed (${response.status})`);
        uploadStatus.textContent = `Indexed ${file.name}. You can ask questions about it now.`;
        messages.innerHTML = welcomeMarkup;
        suggestions.hidden = false;
        await refreshStatus();
    } catch (error) {
        uploadStatus.className = 'error';
        uploadStatus.textContent = error.message;
    } finally {
        uploadButton.disabled = false;
    }
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
