import GoogleMobileAds
import UIKit

@MainActor
final class AppOpenAdManager: NSObject {
    static let shared = AppOpenAdManager()

    private var appOpenAd: AppOpenAd?
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Date?
    private let timeoutInterval: TimeInterval = 4 * 3_600

    func loadAd() async {
        guard !isLoadingAd, !isAdAvailable else { return }
        isLoadingAd = true

        do {
            let ad = try await AppOpenAd.load(
                with: AdConfiguration.appOpenAdUnitID,
                request: Request()
            )
            ad.fullScreenContentDelegate = self
            appOpenAd = ad
            loadTime = Date()
        } catch {
            appOpenAd = nil
            loadTime = nil
        }

        isLoadingAd = false
    }

    func showAdIfAvailable() async {
        guard !isShowingAd else { return }

        guard isAdAvailable, let appOpenAd else {
            await loadAd()
            return
        }

        isShowingAd = true
        appOpenAd.present(from: nil)
    }

    private var isAdAvailable: Bool {
        guard appOpenAd != nil, let loadTime else { return false }
        return Date().timeIntervalSince(loadTime) < timeoutInterval
    }
}

extension AppOpenAdManager: FullScreenContentDelegate {
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        appOpenAd = nil
        isShowingAd = false
        Task { @MainActor in
            await loadAd()
        }
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        appOpenAd = nil
        isShowingAd = false
        Task { @MainActor in
            await loadAd()
        }
    }
}
