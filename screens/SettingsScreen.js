import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  SafeAreaView,
  ScrollView,
  Image,
  ActivityIndicator,
  Modal,
  Switch,
} from "react-native";
// import { Ionicons } from "react-native-vector-icons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import axios from "axios";
import BlackboxLogo from "../assets/BlackboxLogo.js";
import DownloadIcon from "../assets/DownloadIcon.js";
import ShareIcon from "../assets/ShareIcon.js";

const SettingsScreen = ({ navigation }) => {
  const [vehicleData, setVehicleData] = useState(null);
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dropdownVisible, setDropdownVisible] = useState(false);
  const [darkMode, setDarkMode] = useState(false);
  const [fromDate, setFromDate] = useState(new Date());
  const [toDate, setToDate] = useState(new Date());
  const [showFromDatePicker, setShowFromDatePicker] = useState(false);
  const [showToDatePicker, setShowToDatePicker] = useState(false);
  const [scoreData, setScoreData] = useState(null);

  // Fetch token and data
  const fetchProfileData = async () => {
    try {
      const token = await AsyncStorage.getItem("token");
      if (!token) {
        console.error("No token found");
        return;
      }

      // Fetch user data
      const userResponse = await axios.get(
        "https://api.blackboxservice.monster/v2/gumshoe/user",
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      setUserData(userResponse.data.data);

      // Fetch vehicle data
      const vehicleResponse = await axios.get(
        "https://api.blackboxservice.monster/v2/gumshoe/vehicle",
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      setVehicleData(vehicleResponse.data.data);
    } catch (error) {
      console.error("Error fetching data:", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchScoreData = async () => {
    setLoading(true); // Start loading spinner for the button
    navigation.navigate("ScorecardScreen");
    try {
      const fromISO = fromDate.toISOString().split("T")[0];
      const toISO = toDate.toISOString().split("T")[0];
      const driverID = userData.traccarDriverId;

      // const scoreResponse = await axios.get(
      //   `http://scorecard.blackboxservice.monster/get_driver_scores/?driverID=${driverID}`
      // );

      let rawResponse = scoreResponse.data;

      if (typeof rawResponse !== "string") {
        rawResponse = JSON.stringify(rawResponse);
      }

      rawResponse = rawResponse.replace(/NaN/g, "null");

      try {
        const parsedData = JSON.parse(rawResponse);
        setScoreData(parsedData.data); // Set the score data

        // Navigate to the ScorecardScreen
      } catch (error) {
        // console.error("Error parsing JSON:", error);
      }
    } catch (error) {
      // console.error("Error fetching score data:", error);
    } finally {
      setLoading(false); // Stop loading spinner
    }
  };

  useEffect(() => {
    fetchProfileData();
  }, []);

  const toggleDropdown = () => setDropdownVisible(!dropdownVisible);
  const handleLogout = async () => {
    await AsyncStorage.removeItem("token");
    navigation.navigate("Login");
  };

  const handleFromDateChange = (event, selectedDate) => {
    const currentDate = selectedDate || fromDate;
    setShowFromDatePicker(false);
    setFromDate(currentDate);
  };

  const handleToDateChange = (event, selectedDate) => {
    const currentDate = selectedDate || toDate;
    setShowToDatePicker(false);
    setToDate(currentDate);
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#FFFFFF" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.scrollView}>
        {/* Header Section */}
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            style={styles.backButton}
          >
            {/* <Ionicons name="arrow-back" size={24} color="white" /> */}
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Profile</Text>
          <TouchableOpacity onPress={toggleDropdown}>
            {/* <Ionicons name="settings" size={24} color="white" /> */}
          </TouchableOpacity>
        </View>

        {/* Dropdown Menu */}
        {dropdownVisible && (
          <View style={styles.dropdownMenu}>
            <TouchableOpacity
              style={styles.dropdownItem}
              onPress={handleLogout}
            >
              <Text style={styles.dropdownText}>Logout</Text>
            </TouchableOpacity>
            <View style={styles.switchContainer}>
              <Text style={styles.dropdownText}>Dark Mode</Text>
              <Switch
                value={darkMode}
                onValueChange={() => setDarkMode(!darkMode)}
              />
            </View>
          </View>
        )}

        {/* User Information */}
        {userData && (
          <View style={styles.userSection}>
            {userData.driverPhoto ? (
              <Image
                source={{ uri: userData.driverPhoto }}
                style={styles.userImage}
              />
            ) : (
              <View></View>
              // <Ionicons name="person-circle" size={50} color="white" />
            )}
            <View style={styles.userInfo}>
              <Text style={styles.userName}>{userData.driverName}</Text>
              <Text style={styles.userEmail}>{userData.driverEmail}</Text>
            </View>
            {/* <Ionicons name="pencil" size={24} color="white" /> */}
          </View>
        )}

        {/* Vehicle Information */}
        <Text style={styles.sectionTitle}>Vehicle Information</Text>
        {vehicleData && (
          <View style={styles.detailsSection}>
            <View style={styles.detailRow}>
              <View style={styles.detailColumn}>
                <View style={styles.detailItem}>
                  <Text style={styles.detailLabel}>Telematic Device:</Text>
                  <BlackboxLogo />
                </View>
                <View style={styles.detailItem}>
                  <Text style={styles.detailLabel}>Car Model:</Text>
                  <Text style={styles.detailValue}>
                    {vehicleData.vehicleModel}
                  </Text>
                </View>
              </View>
              <View style={styles.detailColumn}>
                <View style={styles.detailItem}>
                  <Text style={styles.detailLabel}>Plate Number:</Text>
                  <Text style={styles.detailValue}>
                    {vehicleData.vehicleRegistrationNumber}
                  </Text>
                </View>
                <View style={styles.detailItem}>
                  <Text style={styles.detailLabel}>Chassis Number:</Text>
                  <Text style={styles.detailValue}>
                    {vehicleData.vehicleVin}
                  </Text>
                </View>
              </View>
            </View>

            <View style={styles.detailRow}>
              <View style={styles.detailColumn}>
                <View style={styles.detailItem}>
                  <Text style={styles.detailLabel}>Brand:</Text>
                  <Text style={styles.detailValue}>
                    {vehicleData.vehicleMake}
                  </Text>
                </View>
              </View>
              <View style={styles.detailColumn}>
                <View style={styles.detailItem}>
                  <Text style={styles.detailLabel}>Year:</Text>
                  <Text style={styles.detailValue}>
                    {vehicleData.vehicleYear}
                  </Text>
                </View>
              </View>
            </View>
          </View>
        )}

        {/* Scorecard System */}
        <View style={styles.scorecardSection}>
          <Text style={styles.sectionTitle}>Scorecard System</Text>

          {/* Fetch Score Button */}
          <TouchableOpacity
            style={styles.fetchScoreButton}
            onPress={fetchScoreData}
            disabled={loading} // Disable button when loading
          >
            {loading ? (
              <ActivityIndicator size="small" color="#FFFFFF" />
            ) : (
              <Text style={styles.fetchScoreButtonText}>Fetch Score</Text>
            )}
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#496F76",
  },
  scrollView: {
    paddingTop: 20, // Adjusted to start from "Vehicle Information"
    paddingBottom: 20,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#496F76",
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#496F76",
  },
  headerTitle: {
    color: "white",
    fontSize: 20,
    fontFamily: "KodchasanLight",
  },
  backButton: {
    marginRight: 10,
  },
  userSection: {
    backgroundColor: "#496F76",
    flexDirection: "row",
    alignItems: "center",
    padding: 20,
    elevation: 4,
    margin: 10,
    borderRadius: 10,
  },
  userImage: {
    width: 50,
    height: 50,
    borderRadius: 25,
  },
  userInfo: {
    flex: 1,
    marginLeft: 10,
  },
  userName: {
    color: "white",
    fontSize: 14,
    fontFamily: "JuliusSansOneRegular",
  },
  userEmail: {
    color: "white",
    fontSize: 12,
    fontFamily: "KodchasanLight",
  },
  sectionTitle: {
    color: "white",
    fontSize: 20,
    textAlign: "center",
    fontFamily: "JuliusSansOneRegular",
    marginVertical: 10,
  },
  detailsSection: {
    backgroundColor: "#496F76",
    borderRadius: 10,
    padding: 10,
    margin: 10,
  },
  detailRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 10,
  },
  detailColumn: {
    flex: 1,
    flexDirection: "column",
    paddingHorizontal: 5,
  },
  detailItem: {
    backgroundColor: "#496F76",
    padding: 10,
    borderRadius: 5,
    marginBottom: 5,
    elevation: 2,
  },
  detailLabel: {
    color: "white",
    fontSize: 14,
    fontFamily: "KodchasanLight",
  },
  detailValue: {
    color: "white",
    fontSize: 14,
    fontFamily: "KodchasanLight",
  },
  policyContainer: {
    backgroundColor: "#496F76",
    padding: 20,
    margin: 10,
    borderRadius: 10,
    alignItems: "center",
  },
  policyText: {
    color: "white",
    fontSize: 18,
    fontFamily: "KodchasanLight",
  },
  scorecardSection: {
    backgroundColor: "#496F76",
    borderRadius: 10,
    padding: 10,
    margin: 10,
  },
  buttonContainer: {
    flexDirection: "row",
    justifyContent: "space-between",
  },
  button: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 10,
    paddingHorizontal: 15,
    borderRadius: 10,
    marginHorizontal: 5,
    elevation: 2,
  },
  downloadButton: {
    backgroundColor: "#007BFF",
  },
  shareButton: {
    backgroundColor: "#28A745",
  },
  buttonText: {
    color: "white",
    fontSize: 14,
    fontFamily: "KodchasanLight",
    marginRight: 10,
  },
  dropdownMenu: {
    position: "absolute",
    top: 60,
    right: 10,
    backgroundColor: "white", // Changed background color to white
    borderRadius: 10,
    padding: 10,
    width: 150,
    zIndex: 10,
    elevation: 5,
  },
  dropdownItem: {
    paddingVertical: 10,
    paddingHorizontal: 15,
    borderBottomWidth: 1,
    borderBottomColor: "#496F76",
  },
  dropdownText: {
    color: "#496F76", // Adjusted text color to fit new background
    fontSize: 14,
    fontFamily: "KodchasanLight",
  },
  switchContainer: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    padding: 10,
  },
  scorecardSection: {
    backgroundColor: "#496F76",
    borderRadius: 8,
    padding: 16,
    marginBottom: 24,
    marginHorizontal: 15,
  },
  datePickerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 16,
  },
  dateButton: {
    backgroundColor: "#383838",
    padding: 10,
    borderRadius: 8,
  },
  dateButtonText: {
    color: "white",
    fontSize: 16,
    fontFamily: "KodchasanLight",
  },
  fetchScoreButton: {
    backgroundColor: "#4CAF50",
    padding: 10,
    borderRadius: 8,
    alignItems: "center",
    marginBottom: 16,
  },
  fetchScoreButtonText: {
    color: "white",
    fontSize: 16,
    // fontWeight: "bold",
    fontFamily: "KodchasanLight",
  },
  scoreDataSection: {
    marginTop: 16,
  },
  scoreDataText: {
    color: "white",
    fontSize: 16,
    marginBottom: 8,
  },
  // loadingContainer: {
  //   flex: 1,
  //   justifyContent: "center",
  //   alignItems: "center",
  //   backgroundColor: "#151515",
  // },
});

export default SettingsScreen;
