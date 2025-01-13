export const accountOptionStyles = {
    container: {
      flex: 1,
      justifyContent: "center",
      backgroundColor: "#2D3447",
    },
    buttonContainer: {
      flex: 0.2,
      margin: 20,
      justifyContent: "space-between",
    },
    button: {
      backgroundColor: "#5470D3",
      borderRadius: 5,
      padding: 16,
      display:'flex',
      flexDirection: 'row',
      width: "100%",
      shadowColor: "black", // Shadow color
      shadowOffset: {
        width: 0,
        height: 10,
      },
      shadowOpacity: 0, // Shadow opacity
      shadowRadius: 5, // Shadow radius
      elevation: 20, // Android elevation
    },
    buttonText: {
      color: "white",
      fontSize: 18,
      width:'70%',
      textAlign:'center'
    },
    buttonWrapper: {
        backgroundColor: "transparent", // Set the background to transparent
        shadowColor: "black", // Shadow color
        shadowOffset: {
          width: 0,
          height: 5,
        },
        shadowOpacity: 0.5, // Shadow opacity
        shadowRadius: 5, // Shadow radius
        elevation: 5, // Android elevation
        overflow: "hidden", // Clip the shadow within the button
        borderRadius: 5, // Make sure to match the button's border radius
      },
      buttonImage: {
        width: 20,
        height: 20,
      }
  };
  