package org.example.project.locations

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.example.project.BuildConfig

// ═══════════════════════════════════════════════════════════════════════════════
// THEME COLORS
// ═══════════════════════════════════════════════════════════════════════════════
private val PrimaryViolet = Color(0xFF7C3AED)
private val SecondaryPurple = Color(0xFF9333EA)
private val SuccessGreen = Color(0xFF10B981)
private val WarningOrange = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)

// ═══════════════════════════════════════════════════════════════════════════════
// AUTOCOMPLETE PREDICTION DATA
// ═══════════════════════════════════════════════════════════════════════════════
data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// SERVICE CENTER TYPES
// ═══════════════════════════════════════════════════════════════════════════════
enum class ServiceCenterType(
    val displayName: String,
    val icon: ImageVector,
    val searchQuery: String,
    val markerColor: Float
) {
    TYRE_SERVICE(
        "Tyre Service Centers",
        Icons.Default.Build,
        "tyre service center tire shop",
        BitmapDescriptorFactory.HUE_VIOLET
    ),
    AIR_PRESSURE(
        "Air Pressure Stations",
        Icons.Default.Speed,
        "petrol station gas station air pump",
        BitmapDescriptorFactory.HUE_BLUE
    ),
    WHEEL_ALIGNMENT(
        "Wheel Alignment",
        Icons.Default.Tune,
        "wheel alignment center",
        BitmapDescriptorFactory.HUE_GREEN
    ),
    CAR_SERVICE(
        "Car Service Centers",
        Icons.Default.CarRepair,
        "car service center auto repair",
        BitmapDescriptorFactory.HUE_ORANGE
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// PLACE DATA CLASS
// ═══════════════════════════════════════════════════════════════════════════════
data class NearbyPlace(
    val id: String,
    val name: String,
    val address: String,
    val latLng: LatLng,
    val rating: Double?,
    val isOpen: Boolean?,
    val distance: Float,
    val type: ServiceCenterType,
    val phoneNumber: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ServiceCenterScreen(
    onBackClick: () -> Unit = {},
    onNavigateToPlace: (NearbyPlace) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize Places API
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
    }
    
    // Location permission state
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // State
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedType by remember { mutableStateOf(ServiceCenterType.TYRE_SERVICE) }
    var nearbyPlaces by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<NearbyPlace?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    // Search/Autocomplete state
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val focusManager = LocalFocusManager.current
    
    // Camera position state
    val cameraPositionState = rememberCameraPositionState()
    
    // Autocomplete search with debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            searchJob?.cancel()
            searchJob = scope.launch {
                delay(300) // Debounce
                isSearching = true
                predictions = getAutocompletePredictions(context, searchQuery, currentLocation)
                isSearching = false
            }
        } else {
            predictions = emptyList()
        }
    }
    
    // Get current location when permission granted
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            getCurrentLocation(context)?.let { location ->
                currentLocation = LatLng(location.latitude, location.longitude)
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(
                            LatLng(location.latitude, location.longitude),
                            14f
                        )
                    )
                )
            }
        }
    }
    
    // Search nearby places when location or type changes
    LaunchedEffect(currentLocation, selectedType) {
        currentLocation?.let { location ->
            isLoading = true
            nearbyPlaces = searchNearbyPlaces(context, location, selectedType)
            isLoading = false
        }
    }
    
    // Bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (!locationPermissions.allPermissionsGranted) {
            // Permission Request UI
            LocationPermissionRequest(
                onRequestPermission = { locationPermissions.launchMultiplePermissionRequest() },
                onBackClick = onBackClick
            )
        } else {
            // Map UI
            Scaffold(
                topBar = {
                    if (showSearchBar) {
                        // Search bar mode
                        PlacesSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onClose = {
                                showSearchBar = false
                                searchQuery = ""
                                predictions = emptyList()
                            },
                            predictions = predictions,
                            isSearching = isSearching,
                            onPredictionClick = { prediction ->
                                scope.launch {
                                    // Fetch place details and navigate
                                    fetchPlaceAndNavigate(
                                        context = context,
                                        placeId = prediction.placeId,
                                        currentLocation = currentLocation,
                                        selectedType = selectedType,
                                        onPlaceFound = { place ->
                                            selectedPlace = place
                                            nearbyPlaces = listOf(place)
                                            cameraPositionState.move(
                                                CameraUpdateFactory.newLatLngZoom(place.latLng, 16f)
                                            )
                                        }
                                    )
                                    showSearchBar = false
                                    searchQuery = ""
                                    predictions = emptyList()
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    } else {
                        // Normal top bar with dropdown
                        ServiceCenterTopBar(
                            selectedType = selectedType,
                            dropdownExpanded = dropdownExpanded,
                            onDropdownClick = { dropdownExpanded = !dropdownExpanded },
                            onTypeSelect = { type ->
                                selectedType = type
                                dropdownExpanded = false
                            },
                            onBackClick = onBackClick,
                            onSearchClick = { showSearchBar = true }
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Google Map
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true,
                            mapType = MapType.NORMAL
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = true,
                            zoomControlsEnabled = false,
                            compassEnabled = true
                        )
                    ) {
                        // Place markers
                        nearbyPlaces.forEach { place ->
                            Marker(
                                state = MarkerState(position = place.latLng),
                                title = place.name,
                                snippet = place.address,
                                icon = BitmapDescriptorFactory.defaultMarker(place.type.markerColor),
                                onClick = {
                                    selectedPlace = place
                                    showBottomSheet = true
                                    true
                                }
                            )
                        }
                    }
                    
                    // Loading indicator
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp),
                            color = PrimaryViolet
                        )
                    }
                    
                    // Nearby places list button
                    FloatingActionButton(
                        onClick = { showBottomSheet = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = PrimaryViolet
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Show list",
                            tint = Color.White
                        )
                    }
                    
                    // Results count chip
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                selectedType.icon,
                                contentDescription = null,
                                tint = PrimaryViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${nearbyPlaces.size} nearby",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom sheet for place list
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = bottomSheetState,
                containerColor = Color.White
            ) {
                PlacesListSheet(
                    places = nearbyPlaces,
                    selectedPlace = selectedPlace,
                    onPlaceClick = { place ->
                        selectedPlace = place
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(place.latLng, 16f)
                            )
                        }
                    },
                    onNavigateClick = { place ->
                        // Open Google Maps with directions
                        val gmmIntentUri = Uri.parse(
                            "google.navigation:q=${place.latLng.latitude},${place.latLng.longitude}&mode=d"
                        )
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Fallback to browser if Google Maps not installed
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${place.latLng.latitude},${place.latLng.longitude}")
                            )
                            context.startActivity(browserIntent)
                        }
                    },
                    onCallClick = { place ->
                        // Place a phone call
                        place.phoneNumber?.let { phone ->
                            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phone")
                            }
                            context.startActivity(callIntent)
                        }
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR WITH DROPDOWN
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceCenterTopBar(
    selectedType: ServiceCenterType,
    dropdownExpanded: Boolean,
    onDropdownClick: () -> Unit,
    onTypeSelect: (ServiceCenterType) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDropdownClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        selectedType.icon,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        selectedType.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (dropdownExpanded) Icons.Default.KeyboardArrowUp 
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
                
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { onDropdownClick() }
                ) {
                    ServiceCenterType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        type.icon,
                                        contentDescription = null,
                                        tint = if (type == selectedType) PrimaryViolet else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        type.displayName,
                                        fontWeight = if (type == selectedType) 
                                            FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = { onTypeSelect(type) },
                            leadingIcon = {
                                if (type == selectedType) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryViolet
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = PrimaryViolet
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// PLACES SEARCH BAR WITH AUTOCOMPLETE
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlacesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    predictions: List<PlacePrediction>,
    isSearching: Boolean,
    onPredictionClick: (PlacePrediction) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Search input bar
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { 
                        Text(
                            "Search tyre shops, service centers...",
                            color = Color.Gray
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close search"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        // Predictions dropdown
        if (predictions.isNotEmpty() || isSearching) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryViolet,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(predictions) { prediction ->
                            PredictionItem(
                                prediction = prediction,
                                onClick = { onPredictionClick(prediction) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PREDICTION LIST ITEM
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PredictionItem(
    prediction: PlacePrediction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = PrimaryViolet,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                prediction.primaryText,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                prediction.secondaryText,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Default.NorthEast,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOCATION PERMISSION REQUEST UI
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LocationPermissionRequest(
    onRequestPermission: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryViolet.copy(alpha = 0.1f),
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Location icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(16.dp, CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryViolet, SecondaryPurple)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Enable Location",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "We need your location to find nearby tyre service centers and air pressure stations",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryViolet
                )
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Allow Location Access",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PLACES LIST BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PlacesListSheet(
    places: List<NearbyPlace>,
    selectedPlace: NearbyPlace?,
    onPlaceClick: (NearbyPlace) -> Unit,
    onNavigateClick: (NearbyPlace) -> Unit,
    onCallClick: (NearbyPlace) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Nearby Places",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = PrimaryViolet.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "${places.size} found",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = PrimaryViolet,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        if (places.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No places found nearby",
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(places) { place ->
                    PlaceListItem(
                        place = place,
                        isSelected = place.id == selectedPlace?.id,
                        onClick = { onPlaceClick(place) },
                        onNavigateClick = { onNavigateClick(place) },
                        onCallClick = { onCallClick(place) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PLACE LIST ITEM
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PlaceListItem(
    place: NearbyPlace,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onCallClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = if (isSelected) PrimaryViolet.copy(alpha = 0.05f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = when (place.type) {
                            ServiceCenterType.TYRE_SERVICE -> PrimaryViolet
                            ServiceCenterType.AIR_PRESSURE -> Color(0xFF3B82F6)
                            ServiceCenterType.WHEEL_ALIGNMENT -> SuccessGreen
                            ServiceCenterType.CAR_SERVICE -> WarningOrange
                        }.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    place.type.icon,
                    contentDescription = null,
                    tint = when (place.type) {
                        ServiceCenterType.TYRE_SERVICE -> PrimaryViolet
                        ServiceCenterType.AIR_PRESSURE -> Color(0xFF3B82F6)
                        ServiceCenterType.WHEEL_ALIGNMENT -> SuccessGreen
                        ServiceCenterType.CAR_SERVICE -> WarningOrange
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    place.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    place.address,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Distance
                    Icon(
                        Icons.Default.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        formatDistance(place.distance),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    // Rating
                    place.rating?.let { rating ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = WarningOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            String.format("%.1f", rating),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // Open status
                    place.isOpen?.let { isOpen ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (isOpen) "Open" else "Closed",
                            fontSize = 12.sp,
                            color = if (isOpen) SuccessGreen else DangerRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Phone number if available
                    place.phoneNumber?.let { phone ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = SuccessGreen
                        )
                    }
                }
            }
            
            // Call button (if phone available)
            if (place.phoneNumber != null) {
                IconButton(onClick = onCallClick) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call",
                        tint = SuccessGreen
                    )
                }
            }
            
            // Navigate button
            IconButton(onClick = onNavigateClick) {
                Icon(
                    Icons.Default.Directions,
                    contentDescription = "Directions",
                    tint = PrimaryViolet
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════
@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: Context): Location? {
    return try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).await()
    } catch (e: Exception) {
        null
    }
}

private suspend fun searchNearbyPlaces(
    context: Context,
    location: LatLng,
    type: ServiceCenterType
): List<NearbyPlace> {
    return try {
        val placesClient = Places.createClient(context)
        
        // Define place fields to fetch (including phone number)
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING,
            Place.Field.CURRENT_OPENING_HOURS,
            Place.Field.NATIONAL_PHONE_NUMBER,
            Place.Field.INTERNATIONAL_PHONE_NUMBER
        )
        
        // Build search request using text search for better results
        val searchRequest = FindAutocompletePredictionsRequest.builder()
            .setQuery(type.searchQuery)
            .setOrigin(location)
            .setCountries("IN") // Limit to India, change as needed
            .build()
        
        val response = placesClient.findAutocompletePredictions(searchRequest).await()
        
        // Fetch details for each prediction
        val places = mutableListOf<NearbyPlace>()
        
        for (prediction in response.autocompletePredictions.take(10)) {
            try {
                val fetchRequest = FetchPlaceRequest.builder(
                    prediction.placeId,
                    placeFields
                ).build()
                
                val placeResponse = placesClient.fetchPlace(fetchRequest).await()
                val place = placeResponse.place
                
                place.location?.let { placeLatLng ->
                    val distance = calculateDistance(
                        location.latitude, location.longitude,
                        placeLatLng.latitude, placeLatLng.longitude
                    )
                    
                    // Check if open - isOpen is a method, not a property
                    val isOpenNow: Boolean? = try {
                        place.isOpen
                    } catch (e: Exception) {
                        null
                    }
                    
                    // Get phone number
                    val phoneNumber = place.nationalPhoneNumber 
                        ?: place.internationalPhoneNumber
                    
                    places.add(
                        NearbyPlace(
                            id = place.id ?: prediction.placeId,
                            name = place.displayName ?: prediction.getPrimaryText(null).toString(),
                            address = place.formattedAddress 
                                ?: prediction.getSecondaryText(null).toString(),
                            latLng = placeLatLng,
                            rating = place.rating,
                            isOpen = isOpenNow,
                            distance = distance,
                            type = type,
                            phoneNumber = phoneNumber
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip this place if fetch fails
            }
        }
        
        // Sort by distance
        places.sortedBy { it.distance }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun calculateDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

private fun formatDistance(meters: Float): String {
    return if (meters < 1000) {
        "${meters.toInt()}m"
    } else {
        String.format("%.1f km", meters / 1000)
    }
}
// ═══════════════════════════════════════════════════════════════════════════════
// AUTOCOMPLETE HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════
private suspend fun getAutocompletePredictions(
    context: Context,
    query: String,
    currentLocation: LatLng?
): List<PlacePrediction> {
    return try {
        val placesClient = Places.createClient(context)
        
        val requestBuilder = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setTypesFilter(listOf(
                PlaceTypes.CAR_REPAIR,
                PlaceTypes.GAS_STATION,
                PlaceTypes.POINT_OF_INTEREST
            ))
        
        // Add location bias if available
        currentLocation?.let { location ->
            requestBuilder.setOrigin(location)
        }
        
        val response = placesClient.findAutocompletePredictions(requestBuilder.build()).await()
        
        response.autocompletePredictions.map { prediction ->
            PlacePrediction(
                placeId = prediction.placeId,
                primaryText = prediction.getPrimaryText(null).toString(),
                secondaryText = prediction.getSecondaryText(null).toString(),
                fullText = prediction.getFullText(null).toString()
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private suspend fun fetchPlaceAndNavigate(
    context: Context,
    placeId: String,
    currentLocation: LatLng?,
    selectedType: ServiceCenterType,
    onPlaceFound: (NearbyPlace) -> Unit
) {
    try {
        val placesClient = Places.createClient(context)
        
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING
        )
        
        val fetchRequest = FetchPlaceRequest.builder(placeId, placeFields).build()
        val response = placesClient.fetchPlace(fetchRequest).await()
        val place = response.place
        
        place.location?.let { placeLatLng ->
            val distance = if (currentLocation != null) {
                calculateDistance(
                    currentLocation.latitude, currentLocation.longitude,
                    placeLatLng.latitude, placeLatLng.longitude
                )
            } else {
                0f
            }
            
            val nearbyPlace = NearbyPlace(
                id = place.id ?: placeId,
                name = place.displayName ?: "Unknown",
                address = place.formattedAddress ?: "",
                latLng = placeLatLng,
                rating = place.rating,
                isOpen = null,
                distance = distance,
                type = selectedType
            )
            
            onPlaceFound(nearbyPlace)
        }
    } catch (e: Exception) {
        // Handle error silently
    }
}