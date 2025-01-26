// BlackboxStackNavigator.js
import React from "react";
import { createStackNavigator } from "@react-navigation/stack";
import FaultMonitorScreen from "../../screens/FaultMonitorScreen";
import BlackboxScreen from "../../screens/BlackboxScreen";
import Dashboard from "../../screens/Dashboard";
// import Dashboard from "../../assets/Dashboard";
// import BlackboxScreen from "../../screens/BlackboxScreen";
// import Dashboard from "../../screens/Dashboard";
// import FaultMonitorScreen from "../../screens/FaultMonitorScreen";

const Stack = createStackNavigator();

const DashboardStackNavigator = () => {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Dashboard1" component={Dashboard} />
      <Stack.Screen name="BlackboxScreen" component={BlackboxScreen} />
      <Stack.Screen name="faultMonitor" component={FaultMonitorScreen} />
    </Stack.Navigator>
  );
};

export default DashboardStackNavigator;
