import React from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  SafeAreaView,
  TouchableOpacity,
} from "react-native";
import { Ionicons } from "react-native-vector-icons";

const FaultDetailsScreen = ({ route, navigation }) => {
  const { faults } = route.params;

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          style={styles.backButton}
        >
          <Ionicons name="arrow-back" size={24} color="white" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Faults Monitor</Text>
        <View style={{ width: 24 }} />
      </View>
      <ScrollView style={styles.scrollView}>
        {faults.map((fault, index) => (
          <View key={index} style={styles.detailCard}>
            <View style={{ display: "flex", flexDirection: "row",alignItems:'center' }}>
              <Text style={styles.titleText}>Fault:</Text>
              <Text style={[styles.contentText,{color:'red',marginHorizontal:5}]}>{fault.fault}</Text>
            </View>
            <View style={{ display: "flex", flexDirection: "row" }}>
              <Text style={styles.titleText}>Solution:</Text>
              <Text style={[styles.contentText,{color:'green',marginHorizontal:5}]}>{fault.solution}</Text>
            </View>
            <View style={{ display: "flex", flexDirection: "row" }}>
              <Text style={styles.titleText}>Status:</Text>
              <Text style={[styles.contentText,{color:'white', marginHorizontal:5}]}>{fault.status}</Text>
            </View>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#2D3447",
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#4F4C6B",
    marginTop: 10,
  },
  headerTitle: {
    color: "white",
    fontSize: 20,
    fontWeight: "bold",
  },
  scrollView: {
    padding: 16,
  },
  detailCard: {
    backgroundColor: "#292E41",
    borderRadius: 10,
    padding: 16,
    marginBottom: 10,
    elevation: 5, // for Android
    shadowColor: "#000", // for iOS
    shadowOffset: { width: 0, height: 2 }, // for iOS
    shadowOpacity: 0.3, // for iOS
    shadowRadius: 4, // for iOS
  },
  titleText: {
    color: "#FFFFFF",
    fontSize: 18,
    fontWeight: "bold",
    // marginBottom: 5,
  },
  contentText: {
    // color: "red",
    fontSize: 16,
    // marginBottom: 15,
  },
});

//   export default styles;

export default FaultDetailsScreen;
