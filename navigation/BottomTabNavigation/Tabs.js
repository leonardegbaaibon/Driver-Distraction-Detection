import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import Dashboard from '../../screens/Dashboard';
import TripsScreen from '../../screens/TripsScreen';
import SettingsScreen from '../../screens/SettingsScreen';

// Import your SVG icons
import DashboardIcon from '../../assets/Dashboard.js';
import DashboardIconFocused from '../../assets/Dashboard2.js';
import TripIcon from '../../assets/Trip.js';
import TripIconFocused from '../../assets/Trip2.js';
import UserIcon from '../../assets/UserIcon.js';
import UserIconFocused from '../../assets/UserIcon2.js';
import DashboardStackNavigator from './DashboardStackNavigator.js';

const Tab = createBottomTabNavigator();

const MyTabs = () => {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarIcon: ({ focused }) => {
          let IconComponent;
          
          if (route.name === 'Dashboard') {
            IconComponent = focused ? DashboardIcon : DashboardIconFocused;
          } else if (route.name === 'Trips') {
            IconComponent = focused ? TripIcon : TripIconFocused;
          } else if (route.name === 'Profile') {
            IconComponent = focused ? UserIcon : UserIconFocused;
          }

          // Return the SVG component
          return <IconComponent width={24} height={24} />;
        },
        tabBarActiveTintColor: 'white',
        tabBarInactiveTintColor: 'transparent',
        tabBarStyle: {
          backgroundColor: '#496F76',
          borderTopColor: 'transparent',
          shadowColor: 'black',
          shadowOffset: { width: 0, height: 1 },
          shadowOpacity: 0.9,
          shadowRadius: 10,
          elevation: 20, // for Android
        },
      })}
    >
      <Tab.Screen name="Dashboard" component={DashboardStackNavigator} />
      <Tab.Screen name="Trips" component={TripsScreen} />
      <Tab.Screen name="Profile" component={SettingsScreen} />
    </Tab.Navigator>
  );
};

export default MyTabs;
