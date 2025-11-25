---
title: Snap Swipe
description: Clean up your Android photo library with confident left/right swipes.
---

<section class="hero">
  <div class="container hero-grid">
    <div class="hero-card">
      <span class="eyebrow">Android · Photo decluttering</span>
      <h1>Clean up your camera roll one decisive swipe at a time.</h1>
      <p class="lead">Snap Swipe shows one photo at a time so you can keep, delete, or share without distractions. Built with Jetpack Compose for speed, clarity, and on-device privacy.</p>
      <div class="cta-row">
        <a class="button primary" href="#get-started">Build & install</a>
        <a class="button secondary" href="{{ "/privacy/" | relative_url }}">Privacy</a>
      </div>
      <div class="chip-row">
        <span class="chip">Swipe keep/delete</span>
        <span class="chip">Up to share</span>
        <span class="chip">Sort newest ↔ oldest</span>
        <span class="chip">Undo safety net</span>
        <span class="chip">Android 8.0+</span>
      </div>
    </div>

    <div class="hero-visual">
      <div class="frame">
        <img class="screenshot" src="{{ "/img/screenshots/Screenshot_20251125_025042_SnapSwipe.jpg" | relative_url }}" alt="Snap Swipe main swipe screen preview">
      </div>
    </div>
  </div>
</section>

<section class="section" id="features">
  <div class="container">
    <h2>Built for fast, confident decisions</h2>
    <p class="muted">Everything is tuned for momentum: single-focus reviewing, responsive gestures, and a minimal overlay that stays out of the photo.</p>
    <div class="feature-grid">
      <div class="card">
        <strong>One-at-a-time focus</strong>
        <p>Review each photo without thumbnails or clutter. When you decide, Snap Swipe moves on so processed shots do not reappear in the same run.</p>
      </div>
      <div class="card">
        <strong>Natural gestures</strong>
        <p>Left-to-right to keep, right-to-left to delete, swipe up to open sharing. Buttons are still there for taps.</p>
      </div>
      <div class="card">
        <strong>Share without leaving</strong>
        <p>Swipe up for a bottom sheet and send the photo via the Android share sheet with read-access granted for you.</p>
      </div>
      <div class="card">
        <strong>Sort your queue</strong>
        <p>Newest-first or oldest-first from Settings. The photo list reloads instantly when you change it.</p>
      </div>
      <div class="card">
        <strong>Undo safety</strong>
        <p>Undo the last action if you swiped too fast, then keep moving through the list.</p>
      </div>
      <div class="card">
        <strong>Respectful by design</strong>
        <p>No accounts or cloud sync. MediaStore permissions are only used to read, delete, and share photos you choose.</p>
      </div>
    </div>
  </div>
</section>

<section class="section" id="how-it-works">
  <div class="container">
    <h2>How Snap Swipe works</h2>
    <ul class="step-list">
      <li class="step">
        <strong>1) Grant photo access</strong>
        <span class="muted">The app requests Android photo permissions and only queries your on-device MediaStore.</span>
      </li>
      <li class="step">
        <strong>2) Swipe with intent</strong>
        <span class="muted">Keep (left-to-right), delete (right-to-left), or open the share sheet (up). End-of-run and empty states keep you oriented.</span>
      </li>
      <li class="step">
        <strong>3) Adjust the order</strong>
        <span class="muted">Switch between newest-first and oldest-first in Settings; the list reloads immediately.</span>
      </li>
      <li class="step">
        <strong>4) Stay in control</strong>
        <span class="muted">Processed photos are removed from the current session, and delete operations use Android’s recommended APIs for safety.</span>
      </li>
    </ul>
  </div>
</section>

<section class="section" id="get-started">
  <div class="container">
    <h2>Get Snap Swipe</h2>
    <p class="muted">Snap Swipe is an open-source Android app. Build locally with Android Studio (API 26+).</p>
    <ul class="step-list">
      <li class="step">
        <strong>Clone the project</strong>
        <span class="muted">Pull the Snapstudio repository from GitHub and open it in Android Studio.</span>
      </li>
      <li class="step">
        <strong>Build & run</strong>
        <span class="muted">Use the included Gradle setup, target API 35, and run on an emulator or physical device.</span>
      </li>
      <li class="step">
        <strong>Review safely</strong>
        <span class="muted">Try the flows with test photos first. Undo and end-of-run states help you keep track.</span>
      </li>
    </ul>
    <div class="cta-row" style="margin-top:16px;">
      <a class="button secondary" href="{{ "/privacy/" | relative_url }}">Review privacy details</a>
    </div>
  </div>
</section>

<section class="section" id="feedback">
  <div class="container">
    <h2>Feedback & issues</h2>
    <p class="muted">Found a bug or have an idea? Open a GitHub issue with the form that fits your need.</p>
    <div class="cta-row" style="margin-top:12px;">
      <a class="button primary" href="https://github.com/LCRH1883/Snapstudio/issues/new?template=bug-report.yml">Report a bug</a>
      <a class="button secondary" href="https://github.com/LCRH1883/Snapstudio/issues/new?template=feature-request.yml">Request a feature</a>
    </div>
  </div>
</section>
