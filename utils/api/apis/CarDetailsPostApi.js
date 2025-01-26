import AsyncStorage from "@react-native-async-storage/async-storage";
import ApiManager from "../ApiManager/Apimanager";


export const CarDetailsPostApi = (data) => {
    const token = AsyncStorage.getItem('token')
  const result = ApiManager('/v2/gumshoe/vehicle', {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      accept: "application/json",
      Authorization: token
    },

    data:data
    
  });
  return result;
};
