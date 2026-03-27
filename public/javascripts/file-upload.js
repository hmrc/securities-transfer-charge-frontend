class FileUploadAuto {
  constructor(form) {
    this.form = form;
    this.retryCount = 0;
    this.startTime = Date.now();
    this.config = {
      retryDelayMs: 1000,
      maxRetries: 30,
      timeoutMs: 30000
    };
    this.currentFileName = null;
  }

  init() {
    console.log("[Upload] init");

    // Prevent default form submission
    this.form.addEventListener("submit", (e) => e.preventDefault());

    const input = this.form.querySelector('input[type="file"]');
    if (!input) {
      return;
    }

    input.addEventListener("change", () => {
      console.log("[Upload] File selected");
      this.upload();
    });
  }

  upload() {
    const fileInput = this.form.querySelector('input[type="file"]');
    const file = fileInput.files[0];
    if (!file) return;

    this.currentFileName = file.name;
    console.log("[Upload] Uploading:", this.currentFileName);

    // Hide file input and label
    const fileUploadWrapper = this.form.querySelector('.govuk-drop-zone');
    const fileUploadLabel = this.form.querySelector('#upload-label')

    if (fileUploadWrapper) fileUploadWrapper.style.display = 'none';
    if (fileUploadLabel) fileUploadLabel.style.display = 'none';

    // Show initial uploading status
    this.showUploadingRow(this.currentFileName);

    // Submit form to hidden iframe (browser handles S3 upload)
    this.submitToIframe();

    // Start polling after slight delay
    setTimeout(() => this.poll(), 1000);
  }

  submitToIframe() {
    this.form.target = "upload-frame";
    this.form.submit();
  }

  // polling function
  poll() {
    const url = this.form.dataset.checkStatusUrl;

    fetch(url)
      .then(res => res.json())
      .then(data => this.handleStatus(data))
      .catch(err => {
        this.showFailureRow("UNKNOWN", this.currentFileName);
      });
  }

  handleStatus(data) {
    if (data.fileStatus === "Ready" || data.fileStatus === "ACCEPTED") {
      this.showSuccessRow(this.currentFileName);
      return;
    }

    if (data.fileStatus === "Failed") {
      this.showFailureRow(data.failureReason, this.currentFileName);
      return;
    }

    // WAITING → retry
    this.retry();
  }

  retry() {
    this.retryCount++;
    const elapsed = Date.now() - this.startTime;
    console.log(`[Upload] Retry ${this.retryCount}, elapsed ${elapsed}ms`);

    if (this.retryCount > this.config.maxRetries || elapsed > this.config.timeoutMs) {
      this.showFailureRow("UNKNOWN", this.currentFileName);
      return;
    }

    setTimeout(() => this.poll(), this.config.retryDelayMs);
  }

  showUploadingRow(fileName) {
    const summary = this.form.querySelector('.upload-summary');
    const liveRegion = this.form.querySelector('#statusInformation');

    if (!summary) return;

    summary.innerHTML = `
      <div class="govuk-summary-list__row">
        <dt class="govuk-summary-list__key">${fileName}</dt>
        <dd class="govuk-summary-list__value">
          <strong class="govuk-tag govuk-tag--yellow">Uploading…</strong>
        </dd>
      </div>
    `;

    if (liveRegion) liveRegion.innerText = `${fileName} status: Uploading…`;
  }

  showSuccessRow(fileName) {
    const summary = this.form.querySelector('.upload-summary');
    const liveRegion = this.form.querySelector('#statusInformation');
    const removeUrl = this.form.dataset.removeUrl || "#";

    summary.innerHTML = `
      <div class="govuk-summary-list__row">
        <dt class="govuk-summary-list__key">${fileName}</dt>
        <dd class="govuk-summary-list__value">
          <strong class="govuk-tag govuk-tag--green">Uploaded</strong>
        </dd>
        <dd class="govuk-summary-list__actions">
          <a href="${removeUrl}" class="govuk-link">Remove<span class="govuk-visually-hidden"> ${fileName}</span></a>
        </dd>
      </div>
    `;

    if (liveRegion) liveRegion.innerText = `${fileName} status: Uploaded`;
  }

  showFailureRow(reason, fileName) {
    const summary = this.form.querySelector('.upload-summary');
    const liveRegion = this.form.querySelector('#statusInformation');
    const removeUrl = this.form.dataset.removeUrl || "#";

    let message = "The selected file has not fully uploaded";
    switch (reason) {
      case "QUARANTINE": message = "The file contains a virus."; break;
      case "REJECTED": message = "The file type is not allowed."; break;
      case "SIZE": message = "The file is too large."; break;
      case "DUPLICATE": message = "This file has already been uploaded."; break;
      case "UNKNOWN": message = "The selected file has not fully uploaded"; break;
    }

    summary.innerHTML = `
      <div class="govuk-summary-list__row">
        <dt class="govuk-summary-list__key">
          <div class="govuk-form-group govuk-form-group--error">
            ${fileName}
            <p id="not-uploaded-error" class="govuk-error-message">
              <span class="govuk-visually-hidden">Error:</span> ${message}
            </p>
          </div>
        </dt>
        <dd class="govuk-summary-list__value">
          <strong class="govuk-tag govuk-tag--yellow">Uploading</strong>
        </dd>
        <dd class="govuk-summary-list__actions">
          <a class="govuk-link" href="${removeUrl}">Remove<span class="govuk-visually-hidden"> ${fileName}</span></a>
        </dd>
      </div>
    `;

    if (liveRegion) liveRegion.innerText = `${fileName} status: ${message}`;

    // Hide file input
    const fileUploadWrapper = this.form.querySelector('.govuk-drop-zone');
    if (fileUploadWrapper) fileUploadWrapper.style.display = 'none';
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const forms = document.querySelectorAll('form[data-module="file-upload-auto"]');
  forms.forEach(form => new FileUploadAuto(form).init());
});