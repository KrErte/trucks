import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class PwaInstallService {
  private deferredPrompt: any = null;
  canInstall = false;

  constructor() {
    window.addEventListener('beforeinstallprompt', (e: Event) => {
      e.preventDefault();
      this.deferredPrompt = e;
      this.canInstall = true;
    });

    window.addEventListener('appinstalled', () => {
      this.canInstall = false;
      this.deferredPrompt = null;
    });
  }

  async install(): Promise<boolean> {
    if (!this.deferredPrompt) return false;
    this.deferredPrompt.prompt();
    const result = await this.deferredPrompt.userChoice;
    this.deferredPrompt = null;
    this.canInstall = false;
    return result.outcome === 'accepted';
  }
}
