import GoogleMobileAds
import SwiftUI

@main
struct TapTranslateAssistantApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        MobileAds.shared.start()
        Task { @MainActor in
            await AppOpenAdManager.shared.loadAd()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .task {
                    await AppOpenAdManager.shared.showAdIfAvailable()
                }
                .onChange(of: scenePhase) { newPhase in
                    guard newPhase == .active else { return }
                    Task { @MainActor in
                        await AppOpenAdManager.shared.showAdIfAvailable()
                    }
                }
        }
    }
}
