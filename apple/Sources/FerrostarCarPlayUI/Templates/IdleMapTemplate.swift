import CarPlay

// TODO: This has yet to be tested/used, but it's here as a starting point for the default idle.
public class IdleMapTemplate: CPMapTemplate {
    private var searchButton: CPBarButton?
    private var recenterButton: CPMapButton?
    private var startNavigationButton: CPMapButton?

    public var onSearchButtonTapped: (() -> Void)?
    public var onRecenterButtonTapped: (() -> Void)?
    public var onStartNavigationButtonTapped: (() -> Void)?

    // MARK: - Initialization

    override public init() {
        super.init()
        setupUI()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: - UI Configuration

    private func setupUI() {
        // Configure search button
        searchButton = CPBarButton(title: "Search", handler: { [weak self] _ in
            self?.onSearchButtonTapped?()
        })

        leadingNavigationBarButtons = [searchButton].compactMap { $0 }

        // Configure map buttons
        setupMapButtons()

        // Configure general template properties
        automaticallyHidesNavigationBar = false
        hidesButtonsWithNavigationBar = false
    }

    private func setupMapButtons() {
        // Recenter button
        recenterButton = CPMapButton { [weak self] _ in
            self?.onRecenterButtonTapped?()
        }
        recenterButton?.image = UIImage(systemName: "location")

        // Start navigation button (hidden by default)
        startNavigationButton = CPMapButton { [weak self] _ in
            self?.onStartNavigationButtonTapped?()
        }
        startNavigationButton?.image = UIImage(systemName: "arrow.triangle.turn.up.right.diamond")
        startNavigationButton?.isHidden = true

        mapButtons = [recenterButton, startNavigationButton].compactMap { $0 }
    }

    // MARK: - Public Interface

    public func showStartNavigationButton(_ show: Bool) {
        startNavigationButton?.isHidden = !show
    }

    public func updateStartNavigationButtonImage(_ image: UIImage?) {
        startNavigationButton?.image = image
    }
}
