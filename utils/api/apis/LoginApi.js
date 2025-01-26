import ApiManager from "../ApiManager/Apimanager";


export const LoginApi = (data) => {
  const result = ApiManager('/v2/gumshoe/login', {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      accept: "application/json",
    },

    data:data
    
  });
  return result;
};
