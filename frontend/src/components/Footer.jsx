import React from 'react'

export default function Footer() {
  return (
   <footer className="footer sm:footer-horizontal footer-center bg-base-300 dark:bg-gray-800 text-base-content dark:text-gray-300 p-4 transition-colors duration-200">
  <aside>
    <p>Copyright © {new Date().getFullYear()} - All right reserved by SpendWise</p>
  </aside>
</footer>
  )
}
