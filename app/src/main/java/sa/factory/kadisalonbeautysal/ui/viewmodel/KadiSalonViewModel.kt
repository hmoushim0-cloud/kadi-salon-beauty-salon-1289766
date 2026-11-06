package sa.factory.kadisalonbeautysal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.kadisalonbeautysal.domain.usecase.*
import sa.factory.kadisalonbeautysal.model.KadiSalonItem
import sa.factory.kadisalonbeautysal.model.KadiSalonOrder
import sa.factory.kadisalonbeautysal.model.KadiSalonTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class KadiSalonUiState(
    val catalog: List<KadiSalonItem> = emptyList(),
    val filteredCatalog: List<KadiSalonItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<KadiSalonItem> = emptyList(),
    val telemetry: KadiSalonTelemetry? = null,
    val lastOrder: KadiSalonOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class KadiSalonViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(KadiSalonUiState())
    val uiState: StateFlow<KadiSalonUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: KadiSalonItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: KadiSalonItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
