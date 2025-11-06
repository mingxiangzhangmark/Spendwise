import React, { useRef, useState, useEffect } from "react";
import {
  FaCamera,
  FaCheckCircle,
  FaExclamationCircle,
  FaExclamationTriangle,
  FaInfoCircle,
  FaTimes,
} from "react-icons/fa";
import { passwordResetService, userService } from "../services/api";

const INPUT_CLASSES =
  "mt-2 w-full rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-900 placeholder:text-gray-300 focus:border-gray-400 focus:outline-none";

const LABEL_CLASSES =
  "text-xs font-semibold uppercase tracking-[0.25em] text-gray-400";

const TOAST_ICON = {
  success: FaCheckCircle,
  error: FaExclamationCircle,
  warning: FaExclamationTriangle,
  info: FaInfoCircle,
};

export default function Account() {
  const [user, setUser] = useState({
    id: null,
    username: "",
    email: "",
    phoneNumber: "", // 注意字段名已更改为驼峰命名
    profilePictureUrl: null, // 注意字段名已更改为驼峰命名
    updatedAt: null
  });
  
  const [loading, setLoading] = useState(false);
  const [profileImage, setProfileImage] = useState(null);
  const [profileImageFile, setProfileImageFile] = useState(null);
  const fileInputRef = useRef(null);
  const [toast, setToast] = useState({ show: false, message: "", type: "info" });
  const [passwordModal, setPasswordModal] = useState({
    show: false,
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  // 获取用户资料
  useEffect(() => {
    const fetchUserData = async () => {
      try {
        const response = await userService.getCurrentUser();
        const userData = response.data;

        // Helper to format full name with a space between first and last name when clamped (e.g., "JohnDoe" -> "John Doe")
        const formatFullName = (name) => {
          if (!name) return "";
          // If already contains space(s), collapse multiple spaces to single and trim
          if (name.includes(" ")) return name.replace(/\s+/g, " ").trim();
          // Insert space before capital letters that follow lowercase letters (camel/pascal case)
          return name.replace(/([a-z])([A-Z])/g, "$1 $2").trim();
        };
        
        // 更新用户数据（将用户名按全名显示格式化）
        setUser({
          id: userData.id,
          username: formatFullName(userData.username),
          email: userData.email,
          phoneNumber: userData.phoneNumber || "",
          profilePictureUrl: userData.profilePictureUrl,
          updatedAt: userData.updatedAt
        });
        
        // 如果有头像URL，设置预览图片
        if (userData.profilePictureUrl) {
          setProfileImage(userData.profilePictureUrl);
        }
      } catch (error) {
        console.error("Error fetching user data:", error);
        showToast("Failed to load user profile", "error");
      }
    };

    fetchUserData();
  }, []);

  const handleInputChange = (event) => {
    const { name, value } = event.target;
    setUser((prev) => ({ ...prev, [name]: value }));
  };

  const handleImageClick = () => {
    fileInputRef.current?.click();
  };

  const handleImageChange = (event) => {
    if (!event.target.files || !event.target.files[0]) {
      return;
    }

    const file = event.target.files[0];
    setProfileImageFile(file);
    
    const imageUrl = URL.createObjectURL(file);
    setProfileImage(imageUrl);
  };

  // 更新用户资料
  const handleUpdateProfile = async (event) => {
    event.preventDefault();
    setLoading(true);

    try {
      // 基本表单验证
      if (!user.username.trim()) {
        showToast("Username cannot be empty", "error");
        setLoading(false);
        return;
      }

      if (!user.email.trim()) {
        showToast("Email cannot be empty", "error");
        setLoading(false);
        return;
      }

      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(user.email)) {
        showToast("Please enter a valid email address", "error");
        setLoading(false);
        return;
      }

      // 如果有新头像，先上传头像
      if (profileImageFile) {
        try {
          await uploadProfilePicture();
          // 短暂延迟确保头像上传处理完成
          await new Promise(resolve => setTimeout(resolve, 500));
        } catch (error) {
          console.error("Error uploading profile picture:", error);
          showToast("Profile updated but failed to upload picture", "warning");
        }
      }

      // 准备更新数据
      const updatedUserData = {
        id: user.id,
        username: user.username,
        email: user.email,
        phoneNumber: user.phoneNumber || null,
        profilePictureUrl: user.profilePictureUrl
      };

      // 发送更新请求
      const response = await userService.updateProfile(updatedUserData);
      
      // 更新本地状态
      setUser(prev => ({
        ...prev,
        ...response.data,
        updatedAt: response.data.updatedAt
      }));
      
      showToast("Profile updated successfully", "success");
    } catch (error) {
      const errorMessage = error.response?.data?.message || "Failed to update profile";
      showToast(errorMessage, "error");
      console.error("Error updating profile:", error);
    } finally {
      setLoading(false);
    }
  };

  // 上传头像
  const uploadProfilePicture = async () => {
    try {
      const formData = new FormData();
      // 确保使用后端期望的字段名称，这可能是"file"或其他名称
      formData.append('picture_file', profileImageFile);
      // 或者尝试
      // formData.append('picture', profileImageFile);
      
      const response = await userService.uploadAvatar(formData);
      
      // 检查响应格式并更新用户头像URL
      if (response.data && response.data.profilePictureUrl) {
        setUser(prev => ({
          ...prev,
          profilePictureUrl: response.data.profilePictureUrl
        }));
        setProfileImage(response.data.profilePictureUrl);
      } else {
        console.log("Response format:", response.data);
        // 如果响应格式不符合预期，尝试从其他字段获取URL
        const pictureUrl = response.data?.url || response.data?.picture || response.data?.profilePictureUrl;
        if (pictureUrl) {
          setUser(prev => ({
            ...prev,
            profilePictureUrl: pictureUrl
          }));
          setProfileImage(pictureUrl);
        }
      }
      
      // 清除文件对象
      setProfileImageFile(null);
    } catch (error) {
      console.error("Error uploading profile picture:", error);
      showToast("Failed to upload profile picture. Please try again.", "error");
    }
  };

  const handleChangePassword = () => {
    setPasswordModal({
      show: true,
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
    });
  };

  const handlePasswordInputChange = (event) => {
    const { name, value } = event.target;
    setPasswordModal((prev) => ({ ...prev, [name]: value }));
  };

  const handlePasswordSubmit = async (event) => {
    event.preventDefault();
    

    if (!passwordModal.currentPassword) {
      showToast("Current password is required", "error");
      return;
    }
    
    if (!passwordModal.newPassword) {
      showToast("New password is required", "error");
      return;
    }
    
    if (passwordModal.newPassword !== passwordModal.confirmPassword) {
      showToast("New passwords do not match", "error");
      return;
    }
    

    if (passwordModal.newPassword.length < 8) {
      showToast("New password must be at least 8 characters long", "error");
      return;
    }
    
    try {
      setLoading(true);

      await passwordResetService.changePassword(
        passwordModal.currentPassword,
        passwordModal.newPassword
      );

      closePasswordModal();

      showToast("Password updated successfully", "success");
    } catch (error) {

      const errorMessage = error.response?.data || "Failed to update password";
      showToast(errorMessage, "error");
      console.error("Error updating password:", error);
    } finally {
      setLoading(false);
    }
  };

  const closePasswordModal = () => {
    setPasswordModal((prev) => ({ ...prev, show: false }));
  };

  const showToast = (message, type = "info") => {
    setToast({ show: true, message, type });
    setTimeout(() => {
      setToast((prev) => ({ ...prev, show: false }));
    }, 3000);
  };

  const initials = () => {
    if (!user.username) {
      return "U";
    }
    const firstLetter = user.username.trim()[0];
    return /[a-z]/i.test(firstLetter) ? firstLetter.toUpperCase() : "U";
  };

  const ToastIcon = TOAST_ICON[toast.type] ?? FaInfoCircle;

  // 备用：格式化更新日期（当前未展示快照面板，保留函数以备后用）
  // const formatDate = (dateString) => {
  //   if (!dateString) return "";
  //   const date = new Date(dateString);
  //   return date.toLocaleString();
  // };

  return (
  <div className="max-w-4xl mx-auto text-gray-900 dark:text-gray-100">
      <div className="mb-10 flex flex-col gap-2">
        <span className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Profile</span>
        <h1 className="text-3xl font-semibold text-gray-900 dark:text-gray-100">Account</h1>
  <p className="text-gray-500 dark:text-gray-400">Manage how SpendWise recognizes you and keep your details up to date.</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-3xl border border-gray-100 dark:border-gray-800 bg-white dark:bg-gray-800 p-6 shadow-sm lg:col-span-3">
          <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
            <button
              type="button"
              onClick={handleImageClick}
              className="relative h-24 w-24 overflow-hidden rounded-full border border-gray-200 bg-gray-100 transition-transform hover:scale-[1.02]"
            >
              {profileImage ? (
                <img
                  src={profileImage}
                  alt="Profile"
                  className="h-full w-full object-cover"
                />
              ) : (
                <span className="flex h-full w-full items-center justify-center bg-gray-900 dark:bg-gray-700 text-3xl font-semibold text-white">
                  {initials()}
                </span>
              )}
              <span className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition-opacity hover:opacity-100">
                <FaCamera className="text-white" />
              </span>
            </button>
            <div className="flex-1">
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Use a clear, high-resolution photo so teammates recognize you easily.
              </p>
              <button
                type="button"
                onClick={handleImageClick}
                className="mt-3 rounded-full border border-gray-200 dark:border-gray-700 px-4 py-2 text-xs font-semibold uppercase tracking-[0.3em] text-gray-600 dark:text-gray-300 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
              >
                Update Photo
              </button>
            </div>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
                className="hidden"
            onChange={handleImageChange}
          />

          <form onSubmit={handleUpdateProfile} className="mt-8 space-y-6">
            <div>
              <label className={`${LABEL_CLASSES} dark:text-gray-400`} htmlFor="username">
                Full Name
              </label>
              <input
                id="username"
                name="username"
                type="text"
                value={user.username}
                onChange={handleInputChange}
                placeholder="e.g., John Doe"
                className={`${INPUT_CLASSES} dark:bg-gray-900 dark:border-gray-700 dark:text-gray-100 dark:placeholder:text-gray-500`}
                required
              />
            </div>

            <div>
              <label className={`${LABEL_CLASSES} dark:text-gray-400`} htmlFor="email">
                Email
              </label>
              <input
                id="email"
                name="email"
                type="email"
                value={user.email}
                onChange={handleInputChange}
                className={`${INPUT_CLASSES} dark:bg-gray-900 dark:border-gray-700 dark:text-gray-100 dark:placeholder:text-gray-500`}
                required
              />
            </div>

            <div>
              <label className={`${LABEL_CLASSES} dark:text-gray-400`} htmlFor="phoneNumber">
                Phone Number
              </label>
              <input
                id="phoneNumber"
                name="phoneNumber"
                type="tel"
                value={user.phoneNumber || ""}
                onChange={handleInputChange}
                className={`${INPUT_CLASSES} dark:bg-gray-900 dark:border-gray-700 dark:text-gray-100 dark:placeholder:text-gray-500`}
              />
            </div>

            <div className="flex flex-col gap-4 pt-4 sm:flex-row sm:items-center sm:justify-between">
              <button
                type="button"
                onClick={handleChangePassword}
                className="w-full rounded-full border border-gray-200 dark:border-gray-700 px-4 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-gray-700 dark:text-gray-300 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700 sm:w-auto"
              >
                Change Password
              </button>
              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-full bg-gray-900 dark:bg-indigo-600 px-4 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-gray-700 dark:hover:bg-indigo-500 sm:w-auto disabled:opacity-50"
              >
                {loading ? (
                  <span className="flex items-center justify-center">
                    <svg className="w-5 h-5 mr-2 animate-spin" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Saving...
                  </span>
                ) : "Save Changes"}
              </button>
            </div>
          </form>
        </section>
      </div>

      {passwordModal.show && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl border border-gray-100 dark:border-gray-800 bg-white dark:bg-gray-800 p-6 shadow-xl">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500">Security</p>
                <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Update Password</h2>
              </div>
              <button
                type="button"
                onClick={closePasswordModal}
                className="rounded-full p-2 text-gray-400 dark:text-gray-500 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-600"
                aria-label="Close"
              >
                <FaTimes />
              </button>
            </div>

            <form onSubmit={handlePasswordSubmit} className="space-y-6">
              <div>
                <label className={`${LABEL_CLASSES} dark:text-gray-400`} htmlFor="currentPassword">
                  Current Password
                </label>
                <input
                  id="currentPassword"
                  name="currentPassword"
                  type="password"
                  value={passwordModal.currentPassword}
                  onChange={handlePasswordInputChange}
                  className={`${INPUT_CLASSES} dark:bg-gray-900 dark:border-gray-700 dark:text-gray-100 dark:placeholder:text-gray-500`}
                  required
                />
              </div>

              <div>
                <label className={`${LABEL_CLASSES} dark:text-gray-400`} htmlFor="newPassword">
                  New Password
                </label>
                <input
                  id="newPassword"
                  name="newPassword"
                  type="password"
                  value={passwordModal.newPassword}
                  onChange={handlePasswordInputChange}
                  className={`${INPUT_CLASSES} dark:bg-gray-900 dark:border-gray-700 dark:text-gray-100 dark:placeholder:text-gray-500`}
                  required
                />
              </div>

              <div>
                <label className={`${LABEL_CLASSES} dark:text-gray-400`} htmlFor="confirmPassword">
                  Confirm Password
                </label>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  value={passwordModal.confirmPassword}
                  onChange={handlePasswordInputChange}
                  className={`${INPUT_CLASSES} dark:bg-gray-900 dark:border-gray-700 dark:text-gray-100 dark:placeholder:text-gray-500`}
                  required
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-full bg-gray-900 dark:bg-indigo-600 px-4 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-gray-700 dark:hover:bg-indigo-500 disabled:opacity-50"
              >
                {loading ? (
                  <span className="flex items-center justify-center">
                    <svg className="w-5 h-5 mr-2 animate-spin" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Updating...
                  </span>
                ) : "Save Password"}
              </button>
            </form>
          </div>
        </div>
      )}

      {toast.show && (
        <div className="toast toast-top toast-end z-50">
          <div
            className={`alert shadow-lg ${
              toast.type === "success"
                ? "alert-success"
                : toast.type === "error"
                  ? "alert-error"
                  : toast.type === "warning"
                    ? "alert-warning"
                    : "alert-info"
            }`}
          >
            <div className="flex items-center gap-2 text-sm">
              <ToastIcon className="text-lg" />
              <span>{toast.message}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
