import Foundation

enum AdConfiguration {
    static var bannerAdUnitID: String {
        #if DEBUG
        return "ca-app-pub-3940256099942544/2435281174"
        #else
        return "ca-app-pub-2030886956293359/2914917493"
        #endif
    }

    static var appOpenAdUnitID: String {
        #if DEBUG
        return "ca-app-pub-3940256099942544/5575463023"
        #else
        return "ca-app-pub-2030886956293359/7981435559"
        #endif
    }
}
