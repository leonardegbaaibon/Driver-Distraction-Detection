import React, { useState, useEffect } from "react";
import {
  FlatList,
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  SafeAreaView,
  ActivityIndicator,
} from "react-native";
import TripEntry from "../components/ReuseableComponents/TripEntry";
import Icon from "react-native-vector-icons/MaterialIcons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import axios from "axios";
import DateTimePicker from "@react-native-community/datetimepicker";

const TripsScreen = ({ navigation }) => {
  const [trips, setTrips] = useState([]);
  const [fromDate, setFromDate] = useState(new Date());
  const [toDate, setToDate] = useState(new Date());
  const [showFromPicker, setShowFromPicker] = useState(false);
  const [showToPicker, setShowToPicker] = useState(false);
  const [isLoading, setIsLoading] = useState(false); // State to control loading indicator

  useEffect(() => {
    if (!showFromPicker && !showToPicker) {
      fetchTrips();
    }
  }, [fromDate, toDate]);

  const fetchTrips = async () => {
    try {
      setIsLoading(true); // Show ActivityIndicator when fetching trips
      const token = await AsyncStorage.getItem("token");
  
      // Fetch trips for today
      const response = await axios.get(
        `https://api.blackboxservice.monster/v2/gumshoe/trips?from=${fromDate.toISOString()}&to=${toDate.toISOString()}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
  
      const tripsData = response.data.data.trips || [];
      setTrips(tripsData);
  
      // If no trips for today, fetch trips from yesterday to today
      if (tripsData.length === 0) {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1); // Set to yesterday
        const today = new Date(); // Today's date
  
        const yesterdayToTodayResponse = await axios.get(
          `https://api.blackboxservice.monster/v2/gumshoe/trips?from=${yesterday.toISOString()}&to=${today.toISOString()}`,
          {
            headers: { Authorization: `Bearer ${token}` },
          }
        );
  
        const yesterdayToTodayTripsData = yesterdayToTodayResponse.data.data.trips || [];
        console.log(yesterdayToTodayTripsData)
        setTrips(yesterdayToTodayTripsData);
  
        // Fetch addresses for each trip
        await fetchAddresses(yesterdayToTodayTripsData);
      } else {
        // Fetch addresses for today's trips
        await fetchAddresses(tripsData);
      }
    } catch (error) {
      console.error("Error fetching trips:", error);
    } finally {
      setIsLoading(false); // Hide ActivityIndicator after fetching trips
    }
  };
  

  // const fetchAddresses = async (tripsData) => {
  //   const apiKey = "1f794bfc47664b09b3ec0a3c20d1e8e3"; 
  //   const updatedTrips = await Promise.all(tripsData.map(async (trip) => {
  //     try {
  //       const [startResponse, endResponse] = await Promise.all([
  //         axios.get(`https://api.opencagedata.com/geocode/v1/json?q=${trip.startLat}+${trip.startLon}&key=${apiKey}`),
  //         axios.get(`https://api.opencagedata.com/geocode/v1/json?q=${trip.endLat}+${trip.endLon}&key=${apiKey}`),
  //       ]);
  //       const startFormatted = startResponse.data.results[0]?.formatted || "";
  //       const endFormatted = endResponse.data.results[0]?.formatted || "";

  //       return {
  //         ...trip,
  //         startAddress: startFormatted,
  //         endAddress: endFormatted,
  //       };
  //     } catch (error) {
  //       console.error("Error fetching addresses:", error);
  //       return {
  //         ...trip,
  //         startAddress: "Address not found",
  //         endAddress: "Address not found",
  //       };
  //     }
  //   }));
  //   setTrips(updatedTrips);
  // };

  const handleFromDateChange = (event, selectedDate) => {
    setShowFromPicker(false);
    if (event.type === "set") {
      setFromDate(selectedDate || fromDate);
    }
  };

  const handleToDateChange = (event, selectedDate) => {
    setShowToPicker(false);
    if (event.type === "set") {
      setToDate(selectedDate || toDate);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            style={styles.backButton}
          >
          <Icon name="arrow-back" size={25} color="white" />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Trip History</Text>
          <View style={styles.placeholder} />
        </View>

        {/* Date Pickers */}
        <View style={styles.datePickerContainer}>
          <TouchableOpacity
            onPress={() => setShowFromPicker(true)}
            style={styles.datePickerButton}
          >
            <Text style={styles.datePickerText}>
              <Text style={{ color: "#333333" }}>From:</Text>
              {fromDate.toDateString()}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => setShowToPicker(true)}
            style={styles.datePickerButton}
          >
            <Text style={styles.datePickerText}>
              <Text style={{ color: "#333333" }}>To:</Text>{" "}
              {toDate.toDateString()}
            </Text>
          </TouchableOpacity>
        </View>

        {showFromPicker && (
          <DateTimePicker
            value={fromDate}
            mode="date"
            display="default"
            onChange={handleFromDateChange}
          />
        )}
        {showToPicker && (
          <DateTimePicker
            value={toDate}
            mode="date"
            display="default"
            onChange={handleToDateChange}
          />
        )}

        {/* Show ActivityIndicator when loading */}
        {isLoading ? (
          <ActivityIndicator
            size="large"
            color="#E9B962"
            style={styles.loader}
          />
        ) : trips.length === 0 ? (
          <Text style={styles.noTripsText}>No trips available</Text>
        ) : (
          <FlatList
            data={trips}
            keyExtractor={(_, index) => index.toString()} // Using index as the key
            renderItem={({ item }) => (
              <TouchableOpacity
                onPress={() =>
                  navigation.navigate("TripDetailsScreen", { trip: item })
                }
              >
                <TripEntry
                  startLocation={item.startAddress || ""} // Use the fetched start address
                  endLocation={item.endAddress || ""} // Use the fetched end address
                  duration={`${(item.duration / 1000 / 60).toFixed(1)} mins`}
                />
              </TouchableOpacity>
            )}
            contentContainerStyle={styles.list}
          />
        )}
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#496F76",
  },
  container: {
    flex: 1,
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
  placeholder: {
    width: 24,
  },
  datePickerContainer: {
    flexDirection: "row",
    justifyContent: "space-around",
    padding: 16,
    backgroundColor: "#496F76",
  },
  datePickerButton: {
    backgroundColor: "#EEE8E0",
    padding: 5,
    borderRadius: 8,
  },
  datePickerText: {
    color: "#333333",
    fontSize: 15,
    fontFamily: "KodchasanRegular",
  },
  list: {
    padding: 16,
  },
  backButton: {
    padding: 8,
  },
  noTripsText: {
    color: "#EEE8E0",
    textAlign: "center",
    marginTop: 20,
    fontSize: 16,
    fontFamily: "KodchasanRegular",
  },
  loader: {
    flex: 1,
    justifyContent: "center",
  },
});

export default TripsScreen;
