# Map module — routes and ids, read from the source

Produced from the app source, not from a device. This is how to find every id and
every route WITHOUT a working emulator — and it is more complete than a device dump,
because a dump only shows what happens to be on screen at that moment.

## Where each thing lives

| What | Where |
|---|---|
| Routes between screens | `res/navigation/map_nav_graph.xml` |
| Every id on a screen | `res/layout/<fragment_layout>.xml` |
| Exact toast / dialog wording | `res/values/strings.xml`, or the literal in the Fragment |
| Whether a view is actually reachable | the Fragment's Kotlin — `visibility`, `isFocusable`, rights gating |

## Routes

| From | Action | To |
|---|---|---|
| `nav_graph` | `action_map_to_locationList` | `locationListFragment` |
| `nav_graph` | `action_map_to_locationForm` | `locationFormFragment` |
| `nav_graph` | `action_map_to_studioZoneList` | `studioZoneListFragment` |
| `nav_graph` | `action_map_to_cityList` | `cityListFragment` |
| `nav_graph` | `action_map_to_headerManager` | `headerManagerFragment` |
| `nav_graph` | `action_map_to_cityForm` | `cityFormFragment` |
| `nav_graph` | `action_locationList_to_locationForm` | `locationFormFragment` |
| `nav_graph` | `action_locationForm_to_headerManager` | `headerManagerFragment` |
| `nav_graph` | `action_studioZoneList_to_zoneForm` | `studioZoneFormFragment` |
| `nav_graph` | `action_cityList_to_cityForm` | `cityFormFragment` |

## Screens, their layouts and their ids

### `mapFragment` — MapFragment

Layout: `fragment_map.xml` — 61 ids

```
  bottomLocationPanel  btnAddCity  btnCallDriverMap  btnCities  btnCloseCityPanel  btnCloseDirections
  btnCloseLocationPanel  btnClosePlacesResult  btnFilter  btnFitAll  btnLocationPill  btnLocationPins
  btnPin  btnPlaceDirections  btnPlacePin  btnSearch  btnShareDirections  btnToggleTutorial
  btnTypes  btnUseMyLocation  btnViewAll  btnZoneList  cardCurrentCity  cityListContainer
  directionsPanel  etCitySearch  etDirectionsFrom  etDirectionsTo  etSearch  fabMapType
  fabSearch  filterPanel  loadingOverlay  map  pinModeBanner  placesResultCard
  rvCarousel  rvCities  rvFilterTypes  rvLocationList  rvSearchResults  searchBar
  toolbar  toolbarScrollView  tutorialBody  tutorialCard  tutorialHeaderRow  tvCitiesBadge
  tvCityName  tvClearFilters  tvCurrentCityAction  tvCurrentCityAddress  tvCurrentCityName  tvDirectionsDistance
  tvDirectionsDuration  tvLocationCount  tvPlaceAddress  tvPlaceCoords  tvPlaceName  tvScrollArrowLeft
  tvScrollArrowRight
```

### `locationListFragment` — LocationListFragment

Layout: `fragment_location_list.xml` — 10 ids

```
  btnAddLocation  btnBackToMap  chipGroupFilter  emptyState  progressBar  rvLocations
  tvCount  tvEmptyMessage  tvEmptyTitle  tvTitle
```

### `locationFormFragment` — LocationFormFragment

Layout: `fragment_location_form.xml` — 17 ids

```
  actvType  btnAddNewType  btnCancel  btnClose  btnSave  chipGroupSubtypes
  etAddress  etDescription  etLat  etLng  etName  etScene
  rvPhotos  sceneLayout  subtypesContainer  tvFormTitle  uploadArea
```

### `studioZoneListFragment` — StudioZoneListFragment

Layout: `fragment_studio_zone_list.xml` — 7 ids

```
  btnAdd  emptyState  etZoneSearch  progressBar  rvZones  tvCount
  tvTitle
```

### `studioZoneFormFragment` — StudioZoneFormFragment

Layout: `fragment_studio_zone_form.xml` — 23 ids

```
  btnCancel  btnCenterPoint  btnClose  btnFindIntersection  btnIntersection  btnSave
  chipGroupRadius  etCenterSearch  etCustomRadius  etStreet1  etStreet2  etZoneName
  layoutCenterPointSearch  layoutIntersection  mapContainer  mapPreview  toggleCenterPointType  tvCenterHint
  tvCityBadge  tvFormTitle  tvIntersectionResult  tvLat  tvLng
```

### `cityListFragment` — CityListFragment

Layout: `fragment_city_list.xml` — 3 ids

```
  btnAdd  rvCities  tvCount
```

### `cityFormFragment` — CityFormFragment

Layout: `fragment_city_form.xml` — 28 ids

```
  btnCancel  btnCenterPoint  btnClose  btnFindIntersection  btnIntersection  btnSave
  chipGroupRadius  divider  etCenterSearch  etCitySearch  etCustomRadius  etStreet1
  etStreet2  layoutCenterPointContent  layoutCenterPointSearch  layoutIntersection  sectionCenterPoint  switchCenterPoint
  toggleCenterPointType  tvCenterHint  tvCenterLat  tvCenterLng  tvCityDesc  tvCityName
  tvFormTitle  tvIntersectionResult  tvLat  tvLng
```

### `headerManagerFragment` — HeaderManagerFragment

Layout: `fragment_header_manager.xml` — 5 ids

```
  btnAdd  etTypeSearch  progressBar  rvHeaders  tvCount
```
