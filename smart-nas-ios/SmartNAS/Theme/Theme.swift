import SwiftUI

extension Color {
    static let appPrimary = Color(red: 0.10, green: 0.46, blue: 0.82) // #1976D2
    static let appSecondary = Color(red: 0.15, green: 0.65, blue: 0.60) // #26A69A
    static let appTertiary = Color(red: 1.0, green: 0.44, blue: 0.26) // #FF7043
    static let appBackground = Color(UIColor.systemGroupedBackground)
    static let appSurface = Color(UIColor.systemBackground)
}

struct CardStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(Color.appSurface)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
}

extension View {
    func cardStyle() -> some View { modifier(CardStyle()) }
}

func imageURL(id: Int, thumb: Bool = true) -> URL? {
    let state = AppState()
    let path = thumb ? "/api/photo/\(id)/thumb" : "/api/photo/\(id)/original"
    return URL(string: "\(