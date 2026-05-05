import GoogleMobileAds
import SwiftUI

struct BannerAdView: UIViewRepresentable {
    let width: CGFloat

    func makeUIView(context: Context) -> BannerView {
        let adSize = currentOrientationAnchoredAdaptiveBanner(width: width)
        let bannerView = BannerView(adSize: adSize)
        bannerView.adUnitID = AdConfiguration.bannerAdUnitID
        bannerView.load(Request())
        return bannerView
    }

    func updateUIView(_ uiView: BannerView, context: Context) {
        let adSize = currentOrientationAnchoredAdaptiveBanner(width: width)
        uiView.adSize = adSize
    }
}
