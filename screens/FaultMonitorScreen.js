import React, { useState, useEffect } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  SafeAreaView,
  ActivityIndicator,
  LayoutAnimation,
  UIManager,
  Platform,
} from "react-native";
// import { Ionicons } from "react-native-vector-icons";
import axios from "axios";
import moment from "moment";
import AsyncStorage from '@react-native-async-storage/async-storage'; // For accessing local storage

// Enable LayoutAnimation on Android
if (Platform.OS === 'android') {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

const FaultMonitorScreen = ({ navigation }) => {
  const [faults, setFaults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedFaults, setExpandedFaults] = useState({}); // To track expanded state

  // Fetch fault data from the API with Authorization token
  useEffect(() => {
    const fetchFaults = async () => {
      try {
        const token = await AsyncStorage.getItem('token');
        if (!token) {
          console.error("No token found");
          return;
        }

        const response = await axios.get("https://api.blackboxservice.monster/v2/gumshoe/faults", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        // Duplicate the fetched faults temporarily
        let fetchedFaults = response.data.data.faults || [];

        // For now, duplicate the data up to five times
        // while (fetchedFaults.length < 5) {
        //   fetchedFaults = [...fetchedFaults, ...fetchedFaults].slice(0, 5);
        // }

        setFaults(fetchedFaults);
      } catch (error) {
        console.error("Error fetching fault data: ", error);
      } finally {
        setLoading(false);
      }
    };

    fetchFaults();
  }, []);

  // Toggle fault item expansion
  const toggleExpand = (index) => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut); // Smooth animation
    setExpandedFaults((prevState) => ({
      ...prevState,
      [index]: !prevState[index], // Toggle the specific fault's expanded state
    }));
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          {/* <Ionicons name="arrow-back" size={24} color="white" /> */}
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Faults Monitor</Text>
        <View style={{ width: 24 }} />
      </View>

      {loading ? (
        <ActivityIndicator size="large" color="#4CAF50" style={styles.loader} />
      ) : (
        <ScrollView style={styles.scrollView}>
          {faults.length > 0 ? (
            faults.map((fault, index) => {
              const isExpanded = expandedFaults[index]; // Check if this fault is expanded
              return (
                <TouchableOpacity key={index} onPress={() => toggleExpand(index)}>
                  <View style={styles.faultItem}>
                    <Text style={styles.faultCode}>Fault Code: {fault.faultCode}</Text>
                    <Text style={styles.detail}>DTC Type: {fault.dtcType}</Text>

                    {isExpanded && (
                      <>
                        <Text style={styles.detail}>Location: {fault.faultLocation}</Text>
                        <Text style={styles.detail}>Possible Causes: {fault.possibleCauses}</Text>
                        <Text style={styles.detail}>Severity: {fault.severity}</Text>
                      </>
                    )}
                  </View>
                </TouchableOpacity>
              );
            })
          ) : (
            <Text style={styles.noFaultsText}>No faults found.</Text>
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#496F76",
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#496F76",
    marginTop: 10,
  },
  headerTitle: {
    color: "white",
    fontFamily: "KodchasanRegular",
    fontSize: 20,
    marginLeft: 20,
  },
  loader: {
    flex: 1,
    justifyContent: "center",
  },
  scrollView: {
    // margin: 10,
  },
  faultItem: {
    backgroundColor: "#496F76",
    padding: 15,
    borderRadius: 10,
    margin: 10,
    elevation: 5,
    shadowColor: "black",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
  },
  faultCode: {
    color: "red", // Changed to red
    fontSize: 16,
    fontFamily: "JuliusSansOneRegular",
  },
  detail: {
    color: "white",
    marginTop: 5,
    fontSize: 14,
    fontFamily: "KodchasanRegular",
  },
  noFaultsText: {
    color: "white",
    textAlign: "center",
    fontFamily: "JuliusSansOneRegular",
    fontSize: 18,
    marginTop: 20,
  },
});

export default FaultMonitorScreen;
