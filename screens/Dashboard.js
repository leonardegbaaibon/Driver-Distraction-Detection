import React, { useState } from "react";
import { View, Text, StyleSheet, SafeAreaView } from "react-native";
import { Svg } from "react-native-svg";
import Speedometer from "../components/CarTelematics/speedometer";
import DashboardBox from "../components/ReuseableComponents/DashboardBox";
import System from "../assets/System";
import Faults from "../assets/faults";
import BottomSheetComponent from "../components/ReuseableComponents/BottomSheet";
import { Image } from "react-native";
// create a component
const Dashboard = ({navigation}) => {
  const [isVisible, setIsVisible] = useState(false);
  const handleBoxPress = () => {
    console.log("Box pressed!");
    setIsVisible(!isVisible); // Toggle visibility of the bottom sheet
  };
  const handleFault = () => {
    navigation.navigate('faultMonitor')
  }
  return (
    <>
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* <Text style={styles.welcomeText}>Welcome, Adedamola</Text> */}
        {/* Speedometer would be a custom component */}
        <Speedometer/>
        {/* Statistics */}
        <View style={styles.statisticsContainer}>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>0 hrs</Text>
            <Text style={styles.statLabel}>Time</Text>
          </View>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>0</Text>
            <Text style={styles.statLabel}>Total Mileage</Text>
          </View>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>0 %</Text>
            <Text style={styles.statLabel}>Driver Score</Text>
          </View>
        </View>
        {/* <FaultsMonitor /> */}
        <View style={{display:'flex',justifyContent:'space-between',flexDirection:'row'}}>
          {/* Other dashboard content... */}
          <DashboardBox
            title="System Status"
            icon={<Image source={require('../assets/Car.png')} style={{ width: 100, height: 75, margin: 8 }} />} 
            onPress={handleBoxPress}
          />
          <DashboardBox
            title="Faults Monitor"
            icon={<Image source={require('../assets/Services.png')}   style={{ width: 100, height: 75, margin: 8 }} />} 
            onPress={handleFault}

            />
          {/* Other dashboard content... */}
        </View>
      </View>
    {isVisible && (
    <BottomSheetComponent isVisible={isVisible} setIsVisible={setIsVisible} />
  )}
      {/* <BottomSheet1 /> */}
    </SafeAreaView>
      </>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#496F76", // Set to a dark background color
  },
  container: {
    flex: 1,
    padding: 20,
    alignItems: "center", // Center children horizontally
  },
  welcomeText: {
    color: "#EEE8E0",
    fontSize: 26,
    // fontWeight: "bold",
    alignSelf: "flex-start", // Align to the left
    marginVertical: 20,
    fontFamily: "JuliusSansOneRegular", // Add vertical margin
  },
  statisticsContainer: {
    flexDirection: "row",
    justifyContent: "space-around",
    width: "100%",
    marginTop: 30,
    marginBottom: 20, // Add space below the stats before the next section
  },
  statItem: {
    padding: 15,
    borderRadius: 10, // Rounded corners for the card-like items
    alignItems: "center", // Center children horizontally and vertically
    justifyContent: "center",
    minWidth: 100, // Minimum width for each stat item
    marginHorizontal: 5, // Add horizontal margin between items
  },
  statValue: {
    color: "#EEE8E0",
    fontSize: 24,
    // fontWeight: "bold",
    marginBottom: 5,
    fontFamily: "KodchasanRegular" // Add a small space between the value and the label
  },
  statLabel: {
    color: "#EEE8E0",
    fontSize: 16,
    opacity: 0.7,
    fontFamily: "KodchasanLight" // Slightly dim the label for better hierarchy
  },
  // ... other styles you may have
});

//make this component available to the app
export default Dashboard;
